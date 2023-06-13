package com.r3.corda.evmbridge

import com.r3.corda.cno.evmbridge.dto.ContractInfo
import com.r3.corda.cno.evmbridge.dto.DepositStatus
import com.r3.corda.cno.evmbridge.dto.TokenSetup
import com.r3.corda.cno.evmbridge.dto.TransactionReceipt
import com.r3.corda.evmbridge.services.ResponseOperation
import net.corda.core.flows.FlowExternalOperation
import java.math.BigInteger

interface IBridge {

    // non-transactional
    fun getHash(erc20Address: String, to: String, amount: BigInteger, hash: ByteArray): FlowExternalOperation<ByteArray>
    fun hashfn(secret: String): FlowExternalOperation<ByteArray>

    // transactional
    fun lock(tokenAddress: String, receiverAddress: String, amount: BigInteger, hash: ByteArray, timeout: BigInteger): FlowExternalOperation<TransactionReceipt>

    fun revertTokens(tokenAddress: String, to: String, amount: BigInteger, hash: ByteArray): FlowExternalOperation<TransactionReceipt>

    fun unlockTokens(tokenAddress: String, amount: BigInteger, secret: String): FlowExternalOperation<TransactionReceipt>

    fun depositStatus(tokenAddress: String, receiver: String, amount: BigInteger, hash: ByteArray, timeout: BigInteger) : FlowExternalOperation<DepositStatus>
}

interface IContracts {
    fun deployReverseERC20(chainId: BigInteger, salt: ByteArray, initCode: ByteArray) : ResponseOperation<TransactionReceipt>

    fun deployReverseERC20(chainId: BigInteger, salt: ByteArray, setup: TokenSetup) : ResponseOperation<TransactionReceipt>

    fun getReverseERC20Address(chainId: BigInteger, salt: ByteArray): FlowExternalOperation<ContractInfo>

}