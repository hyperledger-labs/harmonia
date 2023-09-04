package com.interop.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.DefaultEventEncoder
import com.r3.corda.evminterop.Indexed
import com.r3.corda.evminterop.states.swap.SwapTransactionDetails
import com.r3.corda.evminterop.workflows.swap.BuildAndProposeDraftTransactionFlow
import net.corda.core.contracts.OwnableState
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AbstractParty

@StartableByRPC
@InitiatingFlow
class DraftAssetSwapFlow(
    private val transactionId: SecureHash,
    private val outputIndex: Int,
    private val recipient: AbstractParty,
    private val validator: AbstractParty
) : FlowLogic<SecureHash>() {
    @Suspendable
    override fun call(): SecureHash {
        // bob -> draft assetId: "xxx", to: alice, validators: [charlie], threshold: 1, event: "Transfer(address,address,uint256)", params: [0xaa,0xbb,1]

        // Construct the StateRef of the asset you want to spend
        val inputStateRef = StateRef(transactionId, outputIndex)

        // Retrieve the input state (asset X) from the vault using the StateRef
        val inputStateAndRef = serviceHub.toStateAndRef<OwnableState>(inputStateRef)

        val aliceAddress = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8"
        val bobAddress = "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC"
        val charlieAddress = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266"
        val goldTokenDeployAddress = "0x5FbDB2315678afecb367f032d93F642f64180aa3"

        val amount = 1.toBigInteger()

        // Defines the encoding of an event that transfer an amount of 1 wei from Bob to Alice (signals success)
        val forwardTransferEvent = DefaultEventEncoder.encodeEvent(
            goldTokenDeployAddress,
            "Transfer(address,address,uint256)",
            Indexed(aliceAddress),
            Indexed(bobAddress),
            amount
        )

        // Defines the encoding of an event that transfer an amount of 1 wei from Bob to Bob himself (signals revert)
        val backwardTransferEvent = DefaultEventEncoder.encodeEvent(
            goldTokenDeployAddress,
            "Transfer(address,address,uint256)",
            Indexed(aliceAddress),
            Indexed(aliceAddress),
            amount
        )

        val swapDetails = SwapTransactionDetails(
            senderCordaName = ourIdentity,
            receiverCordaName = serviceHub.identityService.wellKnownPartyFromAnonymous(recipient)
                ?: throw IllegalArgumentException("Unknown party $recipient"),
            cordaAssetState = inputStateAndRef,
            approvedCordaValidators = listOf(
                serviceHub.identityService.wellKnownPartyFromAnonymous(validator)
                    ?: throw IllegalArgumentException("Unknown party $validator")
            ),
            minimumNumberOfEventValidations = 1,
            forwardEvent = forwardTransferEvent,
            backwardEvent = backwardTransferEvent
        )

        val notary = serviceHub.networkMapCache.notaryIdentities.first() // should come from parameters

        val wireTx = subFlow(BuildAndProposeDraftTransactionFlow(swapDetails, notary))
            ?: throw Exception("Failed to crate Draft Transaction")

        return wireTx.id
    }
}