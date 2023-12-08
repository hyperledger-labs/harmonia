package com.interop.flows

import com.interop.flows.internal.TestNetSetup
import com.r3.corda.evminterop.Erc20TransferEventEncoder
import com.r3.corda.evminterop.services.swap.DraftTxService
import com.r3.corda.evminterop.workflows.IssueGenericAssetFlow
import net.corda.core.identity.AbstractParty
import net.corda.node.services.Permissions.Companion.startFlow
import org.junit.Test
import org.web3j.abi.datatypes.Address
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
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
        val assetTx = runFlow(bob, IssueGenericAssetFlow(assetName))

        val draftTxHash = runFlow(bob, DemoDraftAssetSwapBaseFlow(assetTx.txhash, assetTx.index, alice.toParty(), charlie.toParty()))

        val stx = runFlow(bob, SignDraftTransactionByIDFlow(draftTxHash))

        val (txReceipt, leafKey, merkleProof) = transferAndProve(amount, alice, bobAddress)

        runFlow(bob, CollectBlockSignaturesFlow(draftTxHash, txReceipt.blockNumber, true))

        val signatures = bob.services.cordaService(DraftTxService::class.java).blockSignatures(txReceipt.blockNumber)

        assert(signatures.count() == 1)
    }

    @Test
    fun `draft flows can collect multiple block signatures asynchronously`() {

        val assetName = UUID.randomUUID().toString()

        // Create Corda asset owned by Bob
        val assetTx = runFlow(bob, IssueGenericAssetFlow(assetName))

        val draftTxHash = runFlow(bob, DraftAssetSwapBaseFlow(
            transactionId =  assetTx.txhash,
            outputIndex = assetTx.index,
            recipient = alice.toParty(),
            notary = alice.services.networkMapCache.notaryIdentities.first(),
            validators = listOf(charlie.toParty() as AbstractParty, bob.toParty() as AbstractParty),
            signaturesThreshold = 2,
            unlockEvent = transferEventEncoder
        ))

        val stx = runFlow(bob, SignDraftTransactionByIDFlow(draftTxHash))

        val (txReceipt, leafKey, merkleProof) = transferAndProve(amount, alice, bobAddress)

        runFlow(bob, CollectBlockSignaturesFlow(draftTxHash, txReceipt.blockNumber, true))

        val signatures = bob.services.cordaService(DraftTxService::class.java).blockSignatures(txReceipt.blockNumber)

        assert(signatures.count() == 2)
    }

    @Test
    fun `flows can collect multiple notarisation proofs asynchronously`() {

        val assetName = UUID.randomUUID().toString()

        // Create Corda asset owned by Bob
        val assetTx = runFlow(bob, IssueGenericAssetFlow(assetName))

        val draftTxHash = runFlow(bob, DraftAssetSwapBaseFlow(
            transactionId =  assetTx.txhash,
            outputIndex = assetTx.index,
            recipient = alice.toParty(),
            notary = alice.services.networkMapCache.notaryIdentities.first(),
            validators = listOf(charlie.toParty() as AbstractParty, bob.toParty() as AbstractParty),
            signaturesThreshold = 2,
            unlockEvent = transferEventEncoder
        ))

        val stx = runFlow(bob, SignDraftTransactionByIDFlow(draftTxHash))

        // alice collects evm signatures from bob and charlie
        runFlow(alice, CollectNotarizationSignaturesFlow(draftTxHash, true))

        val signatures = alice.services.cordaService(DraftTxService::class.java).notarizationProofs(draftTxHash)

        val signedData = draftTxHash.bytes
        val addresses = signatures.map {
            // Convert the signature bytes to a Sign.SignatureData object
            val signatureData = Sign.SignatureData(
                it[64], // V
                it.copyOfRange(0, 32), // R
                it.copyOfRange(32, 64)  // S
            )

            // Verify the signature and get the signer's address
            val publicKey = Sign.signedMessageToKey(signedData, signatureData)
            val signerAddress = Keys.getAddress(publicKey)

            Address(signerAddress)
        }.toHashSet()

        assert(signatures.count() == 2)
        assert(addresses.containsAll(listOf(Address(charlieAddress), Address(bobAddress))))
    }
}
