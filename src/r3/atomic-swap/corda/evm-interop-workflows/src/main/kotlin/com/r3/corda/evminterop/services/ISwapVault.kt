package org.web3j.generated.contracts

import net.corda.core.flows.FlowExternalOperation
import java.math.BigInteger

interface ISwapVault {

    val contractAddress: String

    fun claimCommitment(swapId: String): FlowExternalOperation<com.r3.corda.evminterop.dto.TransactionReceipt>

    fun revertCommitment(swapId: String): FlowExternalOperation<com.r3.corda.evminterop.dto.TransactionReceipt>

    fun commit(
        swapId: String,
        recipient: String,
        signaturesThreshold: BigInteger
    ): FlowExternalOperation<com.r3.corda.evminterop.dto.TransactionReceipt>

    fun commitWithToken(
        swapId: String,
        tokenAddress: String,
        tokenId: BigInteger,
        amount: BigInteger,
        recipient: String,
        signaturesThreshold: BigInteger
    ): FlowExternalOperation<com.r3.corda.evminterop.dto.TransactionReceipt>

    fun commitWithToken(
        swapId: String,
        tokenAddress: String,
        tokenId: BigInteger,
        amount: BigInteger,
        recipient: String,
        signaturesThreshold: BigInteger,
        signers: List<String>
    ): FlowExternalOperation<com.r3.corda.evminterop.dto.TransactionReceipt>

    fun commitWithToken(
        swapId: String,
        tokenAddress: String,
        amount: BigInteger,
        recipient: String,
        signaturesThreshold: BigInteger
    ): FlowExternalOperation<com.r3.corda.evminterop.dto.TransactionReceipt>

    fun commitWithToken(
        swapId: String,
        tokenAddress: String,
        amount: BigInteger,
        recipient: String,
        signaturesThreshold: BigInteger,
        signers: List<String>
    ): FlowExternalOperation<com.r3.corda.evminterop.dto.TransactionReceipt>

    fun commitmentHash(swapId: String): FlowExternalOperation<ByteArray>
}
