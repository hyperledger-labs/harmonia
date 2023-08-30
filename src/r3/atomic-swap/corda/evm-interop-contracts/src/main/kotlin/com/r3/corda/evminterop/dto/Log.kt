package com.r3.corda.evminterop.dto

import net.corda.core.serialization.CordaSerializable
import java.math.BigInteger

/**
 * A Corda serializable version of the Web3j's Log that acts as a DTO and allows flows to return the
 * Web3j Log object using Corda serialization.
 */
@CordaSerializable
data class Log(
    val removed: Boolean?,
    val logIndex: String?,
    val transactionIndex: String?,
    val transactionHash: String?,
    val blockHash: String?,
    val blockNumber: BigInteger,
    val address: String?,
    val data: String?,
    val type: String?,
    val topics: List<String>?
) {
    override fun toString(): String {
        return ("Log{"
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
