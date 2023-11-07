package com.r3.corda.evminterop.services

import com.r3.corda.evminterop.dto.Block
import com.r3.corda.evminterop.dto.Transaction
import com.r3.corda.evminterop.dto.TransactionReceipt
import com.r3.corda.evminterop.dto.TransactionReceiptLog
import net.corda.core.flows.FlowExternalOperation
import java.math.BigInteger

interface IWeb3 {

    /**
     * Debug API only available on hardhat test networks
     */
    fun evmSetNextBlockTimestamp(timestamp: BigInteger)

    fun getEvents(address: String) : FlowExternalOperation<TransactionReceiptLog>

    /**
     * Retrieve a block header given its block number
     * @param fullTransactionObjects whether or not to retrieve also the transactions included in the block
     */
    fun getBlockByNumber(number: BigInteger, fullTransactionObjects: Boolean) : FlowExternalOperation<Block>

    /**
     * Retrieve a block header given the block hash
     * @param fullTransactionObjects whether or not to retrieve also the transactions included in the block
     */
    fun getBlockByHash(hash: String, fullTransactionObjects: Boolean) : FlowExternalOperation<Block>

    /**
     * Retrieve a transaction given its hash
     */
    fun getTransactionByHash(hash: String) : FlowExternalOperation<Transaction>

    /**
     * Retrieve a transaction receipt given the transaction hash
     */
    fun getTransactionReceiptByHash(hash: String) : FlowExternalOperation<TransactionReceipt>

    /**
     * Retrieve all transaction receipts for a given block
     */
    fun getBlockReceipts(blockNumber: BigInteger) : FlowExternalOperation<List<TransactionReceipt>>

    /**
     * Signs some data using the current EVM identity
     */
    fun signData(data: ByteArray) : ByteArray
}
