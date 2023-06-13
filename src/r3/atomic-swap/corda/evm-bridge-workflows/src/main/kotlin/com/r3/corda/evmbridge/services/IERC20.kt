package com.r3.corda.evmbridge

import com.r3.corda.cno.evmbridge.dto.TransactionReceipt
import net.corda.core.flows.FlowExternalOperation
import java.math.BigInteger

interface IERC20 {
    // ERC20 Extension
    fun name(): FlowExternalOperation<String>
    fun symbol(): FlowExternalOperation<String>
    fun decimals(): FlowExternalOperation<BigInteger>
    fun totalSupply(): FlowExternalOperation<BigInteger>

    // transactional functions
    fun approve(receiverAddress: String, amount: BigInteger): FlowExternalOperation<TransactionReceipt>
    fun transfer(receiverAddress: String, amount: BigInteger): FlowExternalOperation<TransactionReceipt>
    fun transferFrom(senderAddress: String, receiverAddress: String, amount: BigInteger): FlowExternalOperation<TransactionReceipt>

    // non transactional functions
    fun allowance(senderAddress: String, receiverAddress: String): FlowExternalOperation<BigInteger>
    fun balanceOf(address: String): FlowExternalOperation<BigInteger>
}