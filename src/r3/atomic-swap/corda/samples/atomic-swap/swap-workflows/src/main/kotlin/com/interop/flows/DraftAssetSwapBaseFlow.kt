package com.interop.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.*
import com.r3.corda.evminterop.states.swap.SwapTransactionDetails
import com.r3.corda.evminterop.workflows.swap.BuildAndProposeDraftTransactionFlow
import net.corda.core.contracts.OwnableState
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty

/**
 * DraftAssetSwapBaseFlow sets up the initial swap agreement and stores the draft transaction for later access.
 * @param transactionId the transaction hash for a generic asset that will be spent through this new transaction
 * @param outputIndex the output index of the generic asset on the source transaction
 * @param recipient the new owner for the generic asset once this transaction is successfully unlocked
 * @param notary the trusted notary for this transaction
 * @param validators the external entities that are trusted to collect and sign the block headers that can attest the
 *                   expected event once observed
 * @param signaturesThreshold the minimum number of validator signatures that will allow the locked asset to be released
 * @param unlockEvent the expected event that once received and proved will allow to unlock the asset to the recipient
 */
@StartableByRPC
@InitiatingFlow
class DraftAssetSwapBaseFlow(
    private val assetTx: StateRef,
//    private val transactionId: SecureHash,
//    private val outputIndex: Int,
    private val recipient: AbstractParty,
    private val notary: AbstractParty,
    private val validators: List<AbstractParty>,
    private val signaturesThreshold: Int,
    private val unlockEvent: IUnlockEventEncoder
) : FlowLogic<SecureHash>() {
    @Suspendable
    override fun call(): SecureHash {
        val transactionId = assetTx.txhash
        val outputIndex = assetTx.index
        require(signaturesThreshold <= validators.count() && signaturesThreshold > 0)

        val knownNotary = serviceHub.identityService.wellKnownPartyFromAnonymous(notary)
            ?: throw IllegalArgumentException("Unknown notary $notary")

        val knownValidators = validators.map {
            serviceHub.identityService.wellKnownPartyFromAnonymous(it)
                ?: throw IllegalArgumentException("Unknown party $it")
        }

        // Construct the StateRef of the asset you want to spend
        val inputStateRef = StateRef(transactionId, outputIndex)

        // Retrieve the input state (asset X) from the vault using the StateRef
        val inputStateAndRef = serviceHub.toStateAndRef<OwnableState>(inputStateRef)

        val swapDetails = SwapTransactionDetails(
            senderCordaName = ourIdentity,
            receiverCordaName = serviceHub.identityService.wellKnownPartyFromAnonymous(recipient)
                ?: throw IllegalArgumentException("Unknown party $recipient"),
            cordaAssetState = inputStateAndRef,
            approvedCordaValidators = knownValidators,
            minimumNumberOfEventValidations = signaturesThreshold,
            unlockEvent = unlockEvent
        )

        val wireTx = subFlow(BuildAndProposeDraftTransactionFlow(swapDetails, knownNotary))
            ?: throw Exception("Failed to crate Draft Transaction")

        return wireTx.id
    }
}

/**
 * DemoDraftAssetSwapBaseFlow has the same function as the DraftAssetSwapFlow, but includes some pre-defined, hardcoded
 * events and data that are otherwise difficult to pass in a context like demoing from a command line shell.
 */
@StartableByRPC
@InitiatingFlow
class DemoDraftAssetSwapBaseFlow(
    private val assetTx: StateRef,
//    private val transactionId: SecureHash,
//    private val outputIndex: Int,
    private val recipient: AbstractParty,
    private val validator: AbstractParty
) : FlowLogic<SecureHash>() {
    @Suspendable
    override fun call(): SecureHash {
        val aliceAddress = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8"
        val bobAddress = "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC"
        val goldTokenDeployAddress = "0x5FbDB2315678afecb367f032d93F642f64180aa3"

        val transferEventEncoder = Erc20TransferEventEncoder(
            goldTokenDeployAddress, aliceAddress, bobAddress, 1.toBigInteger()
        )

        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        return subFlow(
            DraftAssetSwapBaseFlow(
                assetTx,
                recipient,
                notary,
                listOf(validator),
                1,
                transferEventEncoder
            )
        )
    }
}
