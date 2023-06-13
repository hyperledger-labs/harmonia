package com.r3.corda.evmbridge

import com.r3.corda.cno.evmbridge.dto.Block
import com.r3.corda.cno.evmbridge.dto.Log
import net.corda.core.flows.FlowExternalOperation
import java.math.BigInteger

interface IWeb3 {
    fun evmSetNextBlockTimestamp(timestamp: BigInteger)

    fun getEvents(address: String) : FlowExternalOperation<Log>

    fun getBlockByNumber(number: BigInteger, fullTransactionObjects: Boolean) : FlowExternalOperation<Block>

    fun getBlockByHash(hash: String, fullTransactionObjects: Boolean) : FlowExternalOperation<Block>

    fun getTransactionByHash(hash: String) : FlowExternalOperation<com.r3.corda.cno.evmbridge.dto.Transaction>
}
