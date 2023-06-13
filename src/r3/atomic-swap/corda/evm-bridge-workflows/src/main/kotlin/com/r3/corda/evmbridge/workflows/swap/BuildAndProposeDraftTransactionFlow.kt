package com.r3.corda.evmbridge.workflows.swap

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evmbridge.contracts.swap.LockCommand
import com.r3.corda.evmbridge.services.swap.DraftTxService
import com.r3.corda.evmbridge.states.swap.LockState
import com.r3.corda.evmbridge.states.swap.SwapTransactionDetails
import net.corda.core.contracts.Command
import net.corda.core.contracts.OwnableState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.crypto.CompositeKey
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.unwrap

/**
 * Initiating flow which builds a draft transaction which puts the Corda asset in a locked state.
 * The locking works by encumbering the asset represented by an [OwnableState] with a [LockState].
 * In this locked state, the asset is owned by a composite key built from the original owner and the
 * intended new owner.
 *
 * The draft transaction is sent to the counterparty together with its dependencies for verification.
 */
@StartableByRPC
@InitiatingFlow
class BuildAndProposeDraftTransactionFlow(val swapTxDetails: SwapTransactionDetails,
                                          val notary: Party) : FlowLogic<WireTransaction?>() {

    @Suspendable
    override fun call(): WireTransaction? {
        val lockedAssetState = constructLockedAsset(swapTxDetails.cordaAssetState.state.data, swapTxDetails.receiverCordaName)
        val lockCommand = Command(LockCommand.Lock, listOf(ourIdentity.owningKey))
        val builder = TransactionBuilder(notary = notary)
            .addInputState(swapTxDetails.cordaAssetState)
            .addOutputState(lockedAssetState, notary = notary, encumbrance = 1)
            .addOutputState(LockState(
                swapTxDetails.senderEvmAddress,
                swapTxDetails.cordaAssetState.state.data.owner.owningKey,
                swapTxDetails.receiverEvmAddress, swapTxDetails.receiverCordaName.owningKey,
                swapTxDetails.approvedCordaValidators.map { it.owningKey },
                swapTxDetails.minimumNumberOfEventValidations,
                participants = listOf(ourIdentity, swapTxDetails.receiverCordaName)),
                notary = notary, encumbrance = 0)
            .addCommand(lockCommand)
        builder.verify(serviceHub)

        // Send draft transaction over to receiver
        val session = initiateFlow(swapTxDetails.receiverCordaName)
        val wireTx = builder.toWireTransaction(serviceHub)
        session.send(wireTx)
        // Send dependencies to allow receiver to perform validations
        val wireTxDependencies = wireTx.inputs.map { it.txhash }.toSet() + wireTx.references.map { it.txhash }.toSet()
        wireTxDependencies.forEach {
            serviceHub.validatedTransactions.getTransaction(it)?.let { stx ->
                subFlow(SendTransactionFlow(session, stx))
            }
        }

        // Wait for counterparty to agree or disagree with the draft transaction
        val draftTxVerificationResult = session.receive<Boolean>().unwrap { it }
        return if (draftTxVerificationResult)
            wireTx
        else
            null
    }

    @Suspendable
    private fun constructLockedAsset(asset: OwnableState, newOwner: Party): OwnableState {
        // Build composite key
        val compositeKey =  CompositeKey.Builder()
            .addKey(asset.owner.owningKey, weight = 1)
            .addKey(newOwner.owningKey, weight = 1)
            .build(1)

        return asset.withNewOwner(AnonymousParty(compositeKey)).ownableState
    }
}

/**
 * Responder flow which receives a draft transaction and verifies it before agreeing to it.
 */
@InitiatedBy(BuildAndProposeDraftTransactionFlow::class)
class BuildAndProposeDraftTransactionFlowResponder(val session: FlowSession) : FlowLogic<Unit?>() {

    @Suspendable
    override fun call() {
        // Receive draft transaction
        val wireTx = session.receive<WireTransaction>().unwrap { it }
        // Receive (and validate) draft transaction dependencies
        repeat(wireTx.inputs.size + wireTx.references.size) {
            subFlow(ReceiveTransactionFlow(session, checkSufficientSignatures = true, statesToRecord = StatesToRecord.ALL_VISIBLE))
        }

        // Send back the validation result
        verifyDraftTx(wireTx).also {
            if (it) {
                serviceHub.cordaService(DraftTxService::class.java).saveDraftTx(wireTx)
                // TODO: Upon validating the proposed transaction to lock the Corda asset, the intended receiver commits the EVM asset to the protocol
            }
            session.send(it)
        }
    }

    @Suspendable
    private fun verifyDraftTx(tx: WireTransaction): Boolean {
        try {
            tx.toLedgerTransaction(serviceHub).verify()
        } catch (e: TransactionVerificationException) {
            return false
        }

        return true
    }
}