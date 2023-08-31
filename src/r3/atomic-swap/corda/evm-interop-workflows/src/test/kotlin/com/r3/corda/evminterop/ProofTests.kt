package com.r3.corda.evminterop

import com.r3.corda.evminterop.dto.TransactionReceipt
import com.r3.corda.evminterop.dto.encoded
import com.r3.corda.evminterop.internal.TestNetSetup
import com.r3.corda.evminterop.workflows.Erc20TransferFlow
import com.r3.corda.evminterop.workflows.GetBlockFlow
import com.r3.corda.evminterop.workflows.GetBlockReceiptsFlow
import com.r3.corda.evminterop.workflows.GetTransactionFlow
import com.r3.corda.interop.evm.common.trie.PatriciaTrie
import net.corda.core.utilities.getOrThrow
import org.junit.Test
import org.web3j.crypto.Hash
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpString
import org.web3j.utils.Numeric
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class ProofTests : TestNetSetup() {

    @Test
    fun `can prove inclusion of transaction in a block`() {
        val amount = 1.toBigInteger()
        val eventSignature = "Transfer(address,address,uint256)"
        val signatureHash: String = Numeric.toHexString(Hash.sha3(eventSignature.toByteArray()))

        // create an ERC20 Transaction that will emit a Transfer event
        val transactionReceipt: TransactionReceipt = alice.startFlow(
            Erc20TransferFlow(goldTokenDeployAddress, bobAddress, amount)
        ).getOrThrow()

        // Retrieve the event from the receipt logs. The event will have references to the
        // transaction that generated it, and the block that mined the transaction.
        val transferEvent = transactionReceipt.logs?.single { log ->
            // Expecting an event with 3 Topics
            log.topics != null && log.topics!!.size == 3
            // Topic0 = event signature
            log.topics!![0] == signatureHash &&
            // Topic1 = address indexed `Transfer from`
            log.topics!![1].takeLast(40) == Numeric.cleanHexPrefix(aliceAddress) &&
            // Topic2 = address indexed `Transfer to`
            log.topics!![2].takeLast(40) == Numeric.cleanHexPrefix(bobAddress) &&
            // data = uint256 non-indexed `Transfer amount` - single non-indexed value = 32 bytes long string
            log.data != null && Numeric.toBigInt(log.data) == amount
        }
        assertNotNull(transferEvent, "The expected Transfer event was not found")

        // Got the event from the transaction receipt instead that from event filter
        // Same logic applies after proving the transaction contains the expected event

        // get the transaction that generated the event following the transactionHash from the event
        val eventSourceTx = alice.startFlow(
           GetTransactionFlow(hash = transferEvent?.transactionHash!!)
        ).getOrThrow()

        // get the block that mined the transaction that generated the event following the blockHash from the event
        val eventSourceBlock = alice.startFlow(
            GetBlockFlow(hash = transferEvent.blockHash!!, includeTransactions = true)
        ).getOrThrow()

        // make sure the block contains the transaction that generated the event
        eventSourceBlock.transactions.filter { it.hash == eventSourceTx.hash }.singleOrNull {
            it.hash == Hash.sha3(it.raw) && it.hash == Hash.sha3(eventSourceTx.raw)
        }

        // make sure the block is not tampered with by calculating the transactions trie root
        // and comparing it against the block-header's transactionsRoot
        val trie = PatriciaTrie()
        for(tx in eventSourceBlock.transactions) {
            trie.put(
                RlpEncoder.encode(RlpString.create(tx.transactionIndex.toLong())),
                Numeric.hexStringToByteArray(tx.raw)
            )
        }
        assertEquals(eventSourceBlock.transactionsRoot, Numeric.toHexString(trie.root.hash))
    }

    @Test
    fun `can prove inclusion of event in a block`() {

        // Event expectations are defined ahead of the transaction/event
        val encodedEvent = DefaultEventEncoder.encodeEvent(
            goldTokenDeployAddress,
            "Transfer(address,address,uint256)",
            Indexed(aliceAddress),
            Indexed(bobAddress),
            1.toBigInteger()
        )

        val amount = 1.toBigInteger()
        // create an ERC20 Transaction that will emit a Transfer event
        val transactionReceipt: TransactionReceipt = alice.startFlow(
            Erc20TransferFlow(goldTokenDeployAddress, bobAddress, amount)
        ).getOrThrow()

        val transferEvent = encodedEvent.findIn(transactionReceipt)
        assertNotNull(transferEvent.found, "The expected Transfer event was not found")

        // Got the event from the transaction receipt instead that from event filter
        // Same logic applies after proving the transaction contains the expected event

        // get the block that mined the transaction that generated the event following the blockHash from the event
        val eventSourceBlock = alice.startFlow(
            GetBlockFlow(hash = transferEvent.log.blockHash!!, includeTransactions = true)
        ).getOrThrow()

        val receipts = alice.startFlow(
            GetBlockReceiptsFlow(eventSourceBlock.number)
        ).getOrThrow()

        // make sure the block is not tampered with by calculating the transactions trie root
        // and comparing it against the block-header's transactionsRoot
        val trie = PatriciaTrie()
        for(receipt in receipts) {
            trie.put(
                RlpEncoder.encode(RlpString.create(Numeric.toBigInt(receipt.transactionIndex!!).toLong())),
                receipt.encoded()
            )
        }
        assertEquals(eventSourceBlock.receiptsRoot, Numeric.toHexString(trie.root.hash))

        val txIndex = Numeric.toBigInt(transferEvent.log.transactionIndex!!).toInt()
        val key = RlpEncoder.encode(RlpString.create(txIndex.toLong()))
        val transactionProof = trie.generateMerkleProof(key)

        val verified = PatriciaTrie.verifyMerkleProof(
            Numeric.hexStringToByteArray(eventSourceBlock.receiptsRoot),
            key,
            receipts[txIndex].encoded(),
            transactionProof
        )
        assertTrue(verified)
    }
}
