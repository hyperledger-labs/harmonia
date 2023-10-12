package com.r3.corda.evminterop.services

import com.r3.corda.evminterop.dto.ContractInfo
import com.r3.corda.evminterop.dto.TokenSetup
import com.r3.corda.evminterop.dto.TransactionReceipt
import net.corda.core.flows.FlowExternalOperation
import java.math.BigInteger

interface IContracts {
    fun deployReverseERC20(chainId: BigInteger, salt: ByteArray, initCode: ByteArray) : ResponseOperation<TransactionReceipt>

    fun deployReverseERC20(chainId: BigInteger, salt: ByteArray, setup: TokenSetup) : ResponseOperation<TransactionReceipt>

    fun getReverseERC20Address(chainId: BigInteger, salt: ByteArray): FlowExternalOperation<ContractInfo>
}