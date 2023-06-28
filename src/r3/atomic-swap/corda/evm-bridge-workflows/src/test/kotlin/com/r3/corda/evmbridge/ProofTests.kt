package com.r3.corda.evmbridge

import com.r3.corda.cno.evmbridge.dto.TransactionReceipt
import com.r3.corda.evmbridge.internal.TestNetSetup
import com.r3.corda.evmbridge.workflows.*
import net.corda.core.utilities.getOrThrow
import org.junit.Test
import org.web3j.crypto.Hash
import kotlin.test.assertNotNull
import com.r3.corda.interop.evm.common.trie.PatriciaTrie
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpString
import org.web3j.utils.Numeric
import kotlin.test.assertEquals

class ProofTests : TestNetSetup() {

    private val eventSignature = "Transfer(address,address,uint256)"
    private val signatureHash: String = Numeric.toHexString(Hash.sha3(eventSignature.toByteArray()))
    private val zeroAddress = "0".repeat(64)

    override fun onNetworkSetup() {
    }

    @Test
    fun `can prove inclusion of event in a block`() {
        val amount = 1.toBigInteger()

        // create an ERC20 Transaction that will emit a Transfer event
        val transactionReceipt: TransactionReceipt = alice.startFlow(
            Erc20TransferFlow(goldTokenDeployAddress, bobAddress, amount)
        ).getOrThrow()

        // Retrieve the event from the receipt logs. The event will have references to the
        // transaction that generated it, and the block that mined the transaction.
        val transferEvent = transactionReceipt.logs?.single { log ->
            // Topic0 = event signature
            log.topics?.firstOrNull() == signatureHash &&
            // Topic1 = address indexed `Transfer from`
            log.topics?.getOrElse(1) { _ -> zeroAddress }?.takeLast(40) == Numeric.cleanHexPrefix(aliceAddress) &&
            // Topic2 = address indexed `Transfer to`
            log.topics?.getOrElse(2) { _ -> zeroAddress }?.takeLast(40) == Numeric.cleanHexPrefix(bobAddress) &&
            // data = uint256 non-indexed `Transfer amount` - single non-indexed value = 32 bytes long string
            log.data?.let { Numeric.toBigInt(it) } == amount
        }
        assertNotNull(transferEvent, "Expected Transfer event not found")

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
}