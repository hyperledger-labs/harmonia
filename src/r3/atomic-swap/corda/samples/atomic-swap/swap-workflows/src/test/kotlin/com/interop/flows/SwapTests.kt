package com.interop.flows

import com.interop.flows.internal.TestNetSetup
import com.r3.corda.evminterop.Erc20TransferEventEncoder
import com.r3.corda.evminterop.workflows.*
import net.corda.core.identity.AbstractParty
import org.junit.Test
import org.web3j.utils.Numeric
import java.util.*

class SwapTests : TestNetSetup() {

    private val amount = 1.toBigInteger()

    // Defines the encoding of an event that transfer an amount of 1 wei from Bob to Alice (signals success)
    val transferEventEncoder = Erc20TransferEventEncoder(
        goldTokenDeployAddress, aliceAddress, bobAddress, 1.toBigInteger()
    )

    @Test
    fun `can unlock corda asset by asynchronous collection of block signatures`() {
        val assetName = UUID.randomUUID().toString()

        // Create Corda asset owned by Bob
        val assetTx = runFlow(bob, IssueGenericAssetFlow(assetName))

        val draftTxHash = runFlow(bob, DraftAssetSwapBaseFlow(
            assetTx = assetTx,
            alice.toParty(),
            alice.services.networkMapCache.notaryIdentities.first(),
            listOf(charlie.toParty() as AbstractParty, bob.toParty() as AbstractParty),
            2,
            transferEventEncoder
        ))

        val stx = runFlow(bob, SignDraftTransactionByIDFlow(draftTxHash))

        val (txReceipt, leafKey, merkleProof) = transferAndProve(amount, alice, bobAddress)

        runFlow(bob, CollectBlockSignaturesFlow(draftTxHash, txReceipt.blockNumber, true))

        val utx = runFlow(bob,
            UnlockAssetFlow(
                stx.tx.id,
                txReceipt.blockNumber,
                Numeric.toBigInt(txReceipt.transactionIndex!!)
            )
        )
    }

    @Test
    fun `produce tx events that can be used during demo`() {
        val (txReceipt1, leafKey1, merkleProof1) = transferAndProve(1.toBigInteger(), alice, bobAddress)
        val (txReceipt2, leafKey2, merkleProof2) = transferAndProve(2.toBigInteger(), alice, bobAddress)
    }
}
