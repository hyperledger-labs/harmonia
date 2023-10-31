package com.r3.corda.evminterop

import com.r3.corda.evminterop.dto.encoded
import com.r3.corda.evminterop.internal.TestNetSetup
import com.r3.corda.evminterop.services.swap.DraftTxService
import com.r3.corda.evminterop.states.swap.LockState
import com.r3.corda.evminterop.states.swap.SwapTransactionDetails
import com.r3.corda.evminterop.states.swap.UnlockData
import com.r3.corda.evminterop.workflows.GenericAssetState
import com.r3.corda.evminterop.workflows.IssueGenericAssetFlow
import com.r3.corda.evminterop.workflows.eth2eth.GetBlockFlow
import com.r3.corda.evminterop.workflows.eth2eth.GetBlockReceiptsFlow
import com.r3.corda.evminterop.workflows.swap.BuildAndProposeDraftTransactionFlow
import com.r3.corda.evminterop.workflows.swap.RequestBlockHeaderProofsInitiator
import com.r3.corda.evminterop.workflows.swap.SignDraftTransactionFlow
import com.r3.corda.evminterop.workflows.swap.UnlockTransactionAndObtainAssetFlow
import com.r3.corda.interop.evm.common.trie.PatriciaTrie
import net.corda.core.contracts.OwnableState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpString
import org.web3j.utils.Numeric
import java.util.*
import kotlin.test.assertFailsWith

class SwapTests : TestNetSetup() {

    private val amount = 1.toBigInteger()

    @Test
    fun `can successfully unlock using the tx receipt matching the expected event`() {

        // Simulates the case where, in exchange for an asset transfer on EVM from Alice to Bob there will be an asset
        // released on corda from Bob to Alice that Alice can reclaim herself using the EVM transaction receipt/event

        val assetName = UUID.randomUUID().toString()

        // Create Corda asset owned by Bob
        val assetTx = await(bob.startFlow(IssueGenericAssetFlow(assetName)))

        val asset =
            bob.services.vaultService.queryBy(GenericAssetState::class.java, queryCriteria(assetName)).states.single()

        // Generate swap transaction details. These details are shared by both swap parties and are used to coordinate
        // and identify events
        val swapDetails = SwapTransactionDetails(
            senderCordaName = bob.toParty(),
            receiverCordaName = alice.toParty(),
            cordaAssetState = asset,
            approvedCordaValidators = listOf(charlie.toParty()),
            minimumNumberOfEventValidations = 1,
            unlockEvent = transferEventEncoder
        )

        // Build draft transaction and send it to counterparty for verification
        val draftTx = await(bob.startFlow(BuildAndProposeDraftTransactionFlow(swapDetails, notary.toParty())))
        assertEquals(draftTx!!.id, alice.services.cordaService(DraftTxService::class.java).getDraftTx(draftTx.id)!!.id)
        assertEquals(draftTx!!.id, bob.services.cordaService(DraftTxService::class.java).getDraftTx(draftTx.id)!!.id)

        // We generate an EVM asset transfer on EVM from Alice to Bob and retrieve the transaction receipt with the event
        // logs, and generate a merkle-proof form it (includes the proof's leaf key).
        val (txReceipt, leafKey, merkleProof) = transferAndProve(amount, alice, bobAddress)

        // Bob receives the receipt/event confirming the EVM asset transfer, so it is safe to sign because the event can
        // be used to unlock the asset.
        val signedTx = await(bob.startFlow(SignDraftTransactionFlow(draftTx)))

        // Alice initiates a forward transfer of the Corda asset unlocking it using the proofs she collects from the
        // parties designed to validate/sign the block header for the expected block number
        val lockedAsset = alice.services.vaultService.queryBy(GenericAssetState::class.java, queryCriteria(assetName)).states.single()
        val lockState = alice.services.vaultService.queryBy(LockState::class.java).states.single()

        // Generate proof required for Alice to unlock the lock-state and take ownership of the Corda asset
        val validatorSignatures = await(alice.startFlow(
            RequestBlockHeaderProofsInitiator(txReceipt, listOf(charlie.toParty()))
        ))

        // Get the block that mined the transaction that generated the designated EVM event
        val block = alice.startFlow(GetBlockFlow(txReceipt.blockNumber, true)).getOrThrow()

        // Get all the transaction receipts from the block to build and verify the transaction receipts root
        val receipts = alice.startFlow(GetBlockReceiptsFlow(txReceipt.blockNumber)).getOrThrow()

        // Build the trie
        val trie = PatriciaTrie()
        for(receipt in receipts) {
            trie.put(
                RlpEncoder.encode(RlpString.create(Numeric.toBigInt(receipt.transactionIndex!!).toLong())),
                receipt.encoded()
            )
        }

        // verify the trie against the block's stored receipts root
        val verified = PatriciaTrie.verifyMerkleProof(
            Numeric.hexStringToByteArray(block.receiptsRoot),
            RlpEncoder.encode(RlpString.create(Numeric.toBigInt(txReceipt.transactionIndex!!).toLong())),
            txReceipt.encoded(),
            merkleProof
        )
        assert(verified) { "Failed to verify the receipts root" }

        // Gather the data for the unlock command and create the unlock transaction that moves the asset from Alice
        // to the expected recipient Bob in response to the `forwardTransferEvent` event
        val unlockData = UnlockData(merkleProof, validatorSignatures, block.receiptsRoot, txReceipt)
        val stx = await(alice.startFlow(UnlockTransactionAndObtainAssetFlow(lockedAsset, lockState, unlockData)))

        // Verify the unlocked asset is now owned by Alice and not anymore from Bob
        assertEquals(alice.info.chooseIdentity().owningKey, (stx.tx.outputStates.single() as OwnableState).owner.owningKey)
        // Verify that bob can't see the locked asset anymore
        assert(bob.services.vaultService.queryBy(GenericAssetState::class.java, queryCriteria(assetName)).states.isEmpty())
    }

    @Test
    fun `cannot unlock using the tx receipt not matching the expected event`() {

        // Simulates the case where, in exchange for an asset transfer on EVM from Alice to Bob there will be an asset
        // released on corda from Bob to Alice that Alice can reclaim herself using the EVM transaction receipt/event

        val assetName = UUID.randomUUID().toString()

        // Create Corda asset owned by Bob
        val assetTx = await(bob.startFlow(IssueGenericAssetFlow(assetName)))

        val asset = bob.services.vaultService.queryBy(GenericAssetState::class.java, queryCriteria(assetName)).states.single()

        // Generate swap transaction details. These details are shared by both swap parties and are used to coordinate
        // and identify events
        val swapDetails = SwapTransactionDetails(
            senderCordaName = bob.toParty(),
            receiverCordaName = alice.toParty(),
            cordaAssetState = asset,
            approvedCordaValidators = listOf(charlie.toParty()),
            minimumNumberOfEventValidations = 1,
            unlockEvent = invalidTransferEventEncoder
        )

        // Build draft transaction and send it to counterparty for verification
        val draftTx = await(bob.startFlow(BuildAndProposeDraftTransactionFlow(swapDetails, notary.toParty())))
        assertEquals(draftTx!!.id, alice.services.cordaService(DraftTxService::class.java).getDraftTx(draftTx.id)!!.id)

        // We generate an EVM asset transfer on EVM from Bob to Alice and retrieve the transaction receipt with the event
        // logs, and generate a merkle-proof form it (includes the proof's leaf key).
        // *** Note that this is the opposite of the expected event in terms of sender and recipient of the EVM asset ***
        val (txReceipt, leafKey, merkleProof) = transferAndProve(amount, bob, aliceAddress)

        // Bob receives the receipt/event confirming the EVM asset transfer, so it is safe to sign because the event can
        // be used to unlock the asset.
        val signedTx = await(bob.startFlow(SignDraftTransactionFlow(draftTx)))

        // Alice initiates a forward transfer of the Corda asset unlocking it using the proofs she collects from the
        // parties designed to validate/sign the block header for the expected block number
        val lockedAsset = alice.services.vaultService.queryBy(GenericAssetState::class.java).states.single()

        val lockState = alice.services.vaultService.queryBy(LockState::class.java).states.single()

        // Generate proof required for Alice to unlock the lock-state and take ownership of the Corda asset
        val validatorSignatures = await(alice.startFlow(
            RequestBlockHeaderProofsInitiator(txReceipt, listOf(charlie.toParty()))
        ))

        // Get the block that mined the transaction that generated the designated EVM event
        val block = alice.startFlow(GetBlockFlow(txReceipt.blockNumber, true)).getOrThrow()

        // Get all the transaction receipts from the block to build and verify the transaction receipts root
        val receipts = alice.startFlow(GetBlockReceiptsFlow(txReceipt.blockNumber)).getOrThrow()

        // Build the trie
        val trie = PatriciaTrie()
        for(receipt in receipts) {
            trie.put(
                RlpEncoder.encode(RlpString.create(Numeric.toBigInt(receipt.transactionIndex!!).toLong())),
                receipt.encoded()
            )
        }

        // verify the trie against the block's stored receipts root
        val verified = PatriciaTrie.verifyMerkleProof(
            Numeric.hexStringToByteArray(block.receiptsRoot),
            RlpEncoder.encode(RlpString.create(Numeric.toBigInt(txReceipt.transactionIndex!!).toLong())),
            txReceipt.encoded(),
            merkleProof
        )
        assert(verified) { "Failed to verify the receipts root" }

        // Gather the data for the unlock command and create the unlock transaction that moves the asset from Alice
        // to the expected recipient Bob in response to the `forwardTransferEvent` event
        val unlockData = UnlockData(merkleProof, validatorSignatures, block.receiptsRoot, txReceipt)
        assertFailsWith<TransactionVerificationException.ContractRejection> {
            await(alice.startFlow(UnlockTransactionAndObtainAssetFlow(lockedAsset, lockState, unlockData)))
        }
    }
}
