package com.r3.corda.evminterop

import com.r3.corda.evminterop.internal.TestNetSetup
import com.r3.corda.evminterop.workflows.*
import net.corda.core.utilities.getOrThrow
import org.junit.Assert.assertEquals
import org.junit.Test
import org.web3j.crypto.Hash.sha3
import kotlin.test.assertNotNull

class Web3Tests : TestNetSetup() {

    @Test
    fun `can retrieve transactions by hash`() {

        // create some transaction
        val transactionReceipt = alice.startFlow(
            Erc20TokensApproveFlow(goldTokenDeployAddress, bobAddress, 1.toBigInteger())
        ).getOrThrow()

        assert(transactionReceipt.transactionHash != null)

        // retrieve the full transaction
        val transaction = alice.startFlow(
            GetTransactionFlow(transactionReceipt.transactionHash!!)
        ).getOrThrow()

        assertEquals(transactionReceipt.transactionHash, sha3(transaction.raw))
    }

    @Test
    fun `can retrieve block by hash`() {

        // create some transaction
        val transactionReceipt = alice.startFlow(
            Erc20TokensApproveFlow(goldTokenDeployAddress, bobAddress, 1.toBigInteger())
        ).getOrThrow()

        assert(transactionReceipt.blockHash != null)

        // retrieve the full transaction
        val block = alice.startFlow(
            GetBlockFlow(transactionReceipt.blockHash!!, true)
        ).getOrThrow()

        val transaction = block.transactions.singleOrNull {transaction ->
            transaction.hash == transactionReceipt.transactionHash
        }

        assertEquals(transactionReceipt.blockHash!!, block.hash)
        assertNotNull(transaction)
    }

    @Test
    fun `can retrieve block by number`() {

        // create some transaction
        val transactionReceipt = alice.startFlow(
            Erc20TokensApproveFlow(goldTokenDeployAddress, bobAddress, 1.toBigInteger())
        ).getOrThrow()

        // retrieve the full transaction
        val block = alice.startFlow(
            GetBlockFlow(transactionReceipt.blockNumber, true)
        ).getOrThrow()

        val transaction = block.transactions.singleOrNull {transaction ->
            transaction.hash == transactionReceipt.transactionHash
        }

        assertNotNull(transaction)
    }
}