package com.r3.corda.evminterop.workflows.swap

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.contracts.swap.LockCommand
import com.r3.corda.evminterop.services.swap.DraftTxService
import com.r3.corda.evminterop.states.swap.LockState
import com.r3.corda.evminterop.states.swap.SwapTransactionDetails
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
@Suspendable
@StartableByRPC
@InitiatingFlow
class BuildAndProposeDraftTransactionFlow(
    private val swapTxDetails: SwapTransactionDetails,
    private val notary: Party) : FlowLogic<WireTransaction?>() {

    @Suspendable
    override fun call(): WireTransaction? {
        val lockedAssetState = constructLockedAsset(swapTxDetails.cordaAssetState.state.data, swapTxDetails.receiverCordaName)
        val lockCommand = Command(LockCommand.Lock, listOf(ourIdentity.owningKey))
        val lockState = LockState(
            swapTxDetails.cordaAssetState.state.data.owner.owningKey,
            swapTxDetails.receiverCordaName.owningKey,
            notary.owningKey,
            swapTxDetails.approvedCordaValidators.map { it.owningKey },
            swapTxDetails.minimumNumberOfEventValidations,
            swapTxDetails.unlockEvent,
            participants = listOf(ourIdentity, swapTxDetails.receiverCordaName)
        )

        val builder = TransactionBuilder(notary = notary)
            .addInputState(swapTxDetails.cordaAssetState)
            .addOutputState(lockedAssetState, notary = notary, encumbrance = 1)
            .addOutputState(lockState, notary = notary, encumbrance = 0)
            .addCommand(lockCommand)

        builder.verify(serviceHub)
        val session = initiateFlow(swapTxDetails.receiverCordaName)
        val wireTx = builder.toWireTransaction(serviceHub)
        sendTransactionDetails(session, wireTx)

        val draftTxVerificationResult = session.receive<Boolean>().unwrap { it }
        return handleVerificationResult(draftTxVerificationResult, wireTx)
    }

    @Suspendable
    public fun sendTransactionDetails(session: FlowSession, wireTx: WireTransaction) {
        session.send(wireTx)
        val wireTxDependencies = wireTx.inputs.map { it.txhash }.toSet() + wireTx.references.map { it.txhash }.toSet()
        wireTxDependencies.forEach {
            serviceHub.validatedTransactions.getTransaction(it)?.let { stx ->
                subFlow(SendTransactionFlow(session, stx))
            }
        }
    }

    @Suspendable
    public fun handleVerificationResult(draftTxVerificationResult: Boolean, wireTx: WireTransaction): WireTransaction? {
        return if (draftTxVerificationResult) {
            serviceHub.cordaService(DraftTxService::class.java).saveDraftTx(wireTx)
            wireTx
        } else {
            null
        }
    }

    @Suspendable
    public fun constructLockedAsset(asset: OwnableState, newOwner: Party): OwnableState {
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
@Suspendable
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
                // NOTE: Upon validating the proposed transaction to lock the Corda asset,
                //       the intended receiver commits the EVM asset to the protocol
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
