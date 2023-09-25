package com.interop.flows

import com.interop.flows.internal.TestNetSetup
import com.r3.corda.evminterop.DefaultEventEncoder
import com.r3.corda.evminterop.Indexed
import com.r3.corda.evminterop.workflows.*
import net.corda.core.identity.AbstractParty
import org.junit.Test
import org.web3j.utils.Numeric
import java.util.*

class SwapTests : TestNetSetup() {

    private val amount = 1.toBigInteger()

    // Defines the encoding of an event that transfer an amount of 1 wei from Bob to Alice (signals success)
    private val forwardTransferEvent = DefaultEventEncoder.encodeEvent(
        goldTokenDeployAddress,
        "Transfer(address,address,uint256)",
        Indexed(aliceAddress),
        Indexed(bobAddress),
        amount
    )

    // Defines the encoding of an event that transfer an amount of 1 wei from Bob to Bob himself (signals revert)
    private val backwardTransferEvent = DefaultEventEncoder.encodeEvent(
        goldTokenDeployAddress,
        "Transfer(address,address,uint256)",
        Indexed(aliceAddress),
        Indexed(aliceAddress),
        amount
    )

    @Test
    fun `can unlock corda asset by asynchronous collection of block signatures`() {
        val assetName = UUID.randomUUID().toString()

        // Create Corda asset owned by Bob
        val assetTx = await(bob.startFlow(IssueGenericAssetFlow(assetName)))

        val draftTxHash = await(bob.startFlow(DraftAssetSwapFlow(
            assetTx.txhash,
            assetTx.index,
            alice.toParty(),
            alice.services.networkMapCache.notaryIdentities.first(),
            listOf(charlie.toParty() as AbstractParty, bob.toParty() as AbstractParty),
            2,
            forwardTransferEvent,
            backwardTransferEvent
        )))

        val stx = await(bob.startFlow(SignDraftTransactionByIDFlow(draftTxHash)))

        val (txReceipt, leafKey, merkleProof) = transferAndProve(amount, alice, bobAddress)

        await(bob.startFlow(CollectBlockSignaturesFlow(draftTxHash, txReceipt.blockNumber, false)))

        network?.waitQuiescent()

        val utx = await(bob.startFlow(
            UnlockAssetFlow(
                stx.tx.id,
                txReceipt.blockNumber,
                Numeric.toBigInt(txReceipt.transactionIndex!!)
            )
        ))
    }

    @Test
    fun `produce tx events that can be used during demo`() {
        val (txReceipt1, leafKey1, merkleProof1) = transferAndProve(1.toBigInteger(), alice, bobAddress)
        val (txReceipt2, leafKey2, merkleProof2) = transferAndProve(2.toBigInteger(), alice, bobAddress)
    }
}