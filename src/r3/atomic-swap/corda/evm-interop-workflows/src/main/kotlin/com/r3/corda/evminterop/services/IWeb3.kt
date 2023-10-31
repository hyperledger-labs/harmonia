package com.r3.corda.evminterop.services

import com.r3.corda.evminterop.dto.Block
import com.r3.corda.evminterop.dto.Transaction
import com.r3.corda.evminterop.dto.TransactionReceipt
import com.r3.corda.evminterop.dto.TransactionReceiptLog
import net.corda.core.flows.FlowExternalOperation
import java.math.BigInteger

interface IWeb3 {
    fun evmSetNextBlockTimestamp(timestamp: BigInteger)

    fun getEvents(address: String) : FlowExternalOperation<TransactionReceiptLog>

    fun getBlockByNumber(number: BigInteger, fullTransactionObjects: Boolean) : FlowExternalOperation<Block>

    fun getBlockByHash(hash: String, fullTransactionObjects: Boolean) : FlowExternalOperation<Block>

    fun getTransactionByHash(hash: String) : FlowExternalOperation<Transaction>

    fun getTransactionReceiptByHash(hash: String) : FlowExternalOperation<TransactionReceipt>

    fun getBlockReceipts(blockNumber: BigInteger) : FlowExternalOperation<List<TransactionReceipt>>

    fun signData(data: ByteArray) : ByteArray
}
