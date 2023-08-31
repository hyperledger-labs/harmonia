package com.r3.corda.evminterop

import com.r3.corda.evminterop.dto.*
import com.r3.corda.evminterop.internal.TestNetSetup
import com.r3.corda.evminterop.workflows.*
import com.r3.corda.evminterop.workflows.swap.*
import com.r3.corda.interop.evm.common.trie.*
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.StartedMockNode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpString
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*

class SwapTests : TestNetSetup() {

    private val amount = 1.toBigInteger()

    @Test
    fun `expected event can unlock corda asset`() {
        val assetName = UUID.randomUUID().toString()

        // Create Corda asset owned by Bob
        val assetTx = await(bob.startFlow(IssueGenericAssetFlow(assetName)))

        val draftTxHash = await(bob.startFlow(DraftAssetSwapFlow(assetTx.txhash, assetTx.index, alice.toParty(), alice.toParty())))

        val stx = await(bob.startFlow(SignDraftTransactionByIDFlow(draftTxHash)))

        val (txReceipt, leafKey, merkleProof) = transferAndProve(amount, alice, bobAddress)

        val utx = await(bob.startFlow(UnlockAssetFlow(
            stx.tx.id,
            txReceipt.blockNumber.toInt(),
            Numeric.toBigInt(txReceipt.transactionIndex!!).toInt()
        )))
    }

    // Helper function to transfer an EVM asset and produce a merkle proof from the transaction's receipt.
    private fun transferAndProve(amount: BigInteger, senderNode: StartedMockNode, recipientAddress: String) : Triple<TransactionReceipt, ByteArray, SimpleKeyValueStore> {

        // create an ERC20 Transaction from alice to bob that will emit a Transfer event for the given amount
        val transactionReceipt: TransactionReceipt = senderNode.startFlow(
            Erc20TransferFlow(goldTokenDeployAddress, recipientAddress, amount)
        ).getOrThrow()

        // get the block that mined the ERC20 `Transfer` Transaction
        val block = senderNode.startFlow(
            GetBlockFlow(transactionReceipt.blockNumber, true)
        ).getOrThrow()

        // get all transaction receipts from the block that mined the ERC20 `Transfer` Transaction
        val receipts = senderNode.startFlow(
            GetBlockReceiptsFlow(transactionReceipt.blockNumber)
        ).getOrThrow()

        // Build the Patricia Trie from the Block receipts and verify it's valid
        val trie = PatriciaTrie()
        for(receipt in receipts) {
            trie.put(
                RlpEncoder.encode(RlpString.create(Numeric.toBigInt(receipt.transactionIndex!!).toLong())),
                receipt.encoded()
            )
        }
        assertEquals(block.receiptsRoot, Numeric.toHexString(trie.root.hash))

        // generate a proof for the transaction receipt that belong to the ERC20 transfer transaction
        val transferKey = RlpEncoder.encode(RlpString.create(Numeric.toBigInt(transactionReceipt.transactionIndex!!).toLong()))
        val transactionProof = trie.generateMerkleProof(transferKey) as SimpleKeyValueStore

        return Triple(transactionReceipt, transferKey, transactionProof)
    }
}