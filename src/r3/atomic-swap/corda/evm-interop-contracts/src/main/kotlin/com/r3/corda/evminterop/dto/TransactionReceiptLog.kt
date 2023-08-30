package com.r3.corda.evminterop.dto

import net.corda.core.serialization.CordaSerializable
import org.web3j.crypto.Hash
import java.math.BigInteger

/**
 * A Corda serializable version of the Web3j's TransactionReceipt Log that acts as a DTO and allows flows to return the
 * ethereum transaction receipt Log object using Corda serialization.
 */
@CordaSerializable
data class TransactionReceiptLog(
    val removed: Boolean = false,
    val logIndex: String? = null,
    val transactionIndex: String? = null,
    val transactionHash: String? = null,
    val blockHash: String? = null,
    val blockNumber: BigInteger = BigInteger.ZERO,
    val address: String? = null,
    val data: String? = null,
    val type: String? = null,
    val topics: List<String>? = null
) {
    fun eventHash(): ByteArray {
        val builder = StringBuilder()
        topics?.forEach { topic ->
            builder.append(topic ?: "")
        }
        builder.append(data ?: "")
        return Hash.sha3(builder.toString().toByteArray())
    }

    override fun toString(): String {
        return ("TransactionReceiptLog{"
                + "removed="
                + removed
                + ", logIndex='"
                + logIndex
                + '\''
                + ", transactionIndex='"
                + transactionIndex
                + '\''
                + ", transactionHash='"
                + transactionHash
                + '\''
                + ", blockHash='"
                + blockHash
                + '\''
                + ", blockNumber='"
                + blockNumber
                + '\''
                + ", address='"
                + address
                + '\''
                + ", data='"
                + data
                + '\''
                + ", type='"
                + type
                + '\''
                + ", topics="
                + topics
                + '}')
    }
}

