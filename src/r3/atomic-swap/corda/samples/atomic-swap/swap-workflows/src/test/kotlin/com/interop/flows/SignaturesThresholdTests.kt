package com.interop.flows

import com.interop.flows.internal.TestNetSetup
import com.r3.corda.evminterop.DefaultEventEncoder
import com.r3.corda.evminterop.Erc20TransferEventEncoder
import com.r3.corda.evminterop.Indexed
import com.r3.corda.evminterop.services.swap.DraftTxService
import com.r3.corda.evminterop.workflows.IssueGenericAssetFlow
import net.corda.core.identity.AbstractParty
import org.junit.Test
import java.util.*

class SignaturesThresholdTests : TestNetSetup() {

    private val amount = 1.toBigInteger()

    // Defines the encoding of an event that transfer an amount of 1 wei from Bob to Alice (signals success)
    val transferEventEncoder = Erc20TransferEventEncoder(
        goldTokenDeployAddress, aliceAddress, bobAddress, 1.toBigInteger()
    )

    @Test
    fun `demo flow can collect charlie's block signature`() {

        val assetName = UUID.randomUUID().toString()

        // Create Corda asset owned by Bob
        val assetTx = await(bob.startFlow(IssueGenericAssetFlow(assetName)))

        val draftTxHash = await(bob.startFlow(DemoDraftAssetSwapFlow(assetTx.txhash, assetTx.index, alice.toParty(), charlie.toParty())))

        val stx = await(bob.startFlow(SignDraftTransactionByIDFlow(draftTxHash)))

        val (txReceipt, leafKey, merkleProof) = transferAndProve(amount, alice, bobAddress)

        await(bob.startFlow(CollectBlockSignaturesFlow(draftTxHash, txReceipt.blockNumber, true)))

        network?.waitQuiescent()

        val signatures = bob.services.cordaService(DraftTxService::class.java).blockSignatures(txReceipt.blockNumber)

        assert(signatures.count() == 1)
    }

    @Test
    fun `draft flows can collect multiple block signatures asynchronously`() {

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
            transferEventEncoder
        )))

        val stx = await(bob.startFlow(SignDraftTransactionByIDFlow(draftTxHash)))

        val (txReceipt, leafKey, merkleProof) = transferAndProve(amount, alice, bobAddress)

        await(bob.startFlow(CollectBlockSignaturesFlow(draftTxHash, txReceipt.blockNumber, false)))

        network?.waitQuiescent()

        val signatures = bob.services.cordaService(DraftTxService::class.java).blockSignatures(txReceipt.blockNumber)

        assert(signatures.count() == 2)
    }
}