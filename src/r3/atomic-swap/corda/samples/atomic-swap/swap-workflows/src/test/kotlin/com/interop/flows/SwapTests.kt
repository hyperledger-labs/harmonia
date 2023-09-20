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
            txReceipt.blockNumber.toInt(),
            Numeric.toBigInt(txReceipt.transactionIndex!!).toInt()
        )
        ))
    }

    @Test
    fun `produce tx events that can be used during demo`() {
        val (txReceipt1, leafKey1, merkleProof1) = transferAndProve(1.toBigInteger(), alice, bobAddress)
        val (txReceipt2, leafKey2, merkleProof2) = transferAndProve(2.toBigInteger(), alice, bobAddress)
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
        )
    }
}