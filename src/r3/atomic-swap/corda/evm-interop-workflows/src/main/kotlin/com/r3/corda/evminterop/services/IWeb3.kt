package com.r3.corda.evminterop

import com.r3.corda.evminterop.dto.*
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
}
