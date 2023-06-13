package com.r3.corda.cno.evmbridge.dto

import net.corda.core.serialization.CordaSerializable
import java.math.BigInteger

/**
 * A Corda serializable version of the Web3j's TransactionReceipt that acts as a DTO and allows flows to return the
 * ethereum transaction receipt object using Corda serialization.
 */
@CordaSerializable
data class TransactionReceipt(
    val transactionHash: String? = null,
    val transactionIndex: String? = null,
    val blockHash: String? = null,
    val blockNumber: BigInteger,
    val cumulativeGasUsed: String? = null,
    val gasUsed: String? = null,
    val contractAddress: String? = null,
    val root: String? = null,
    val status: String? = null,
    val from: String? = null,
    val to: String? = null,
    val logs: List<TransactionReceiptLog>? = null,
    val logsBloom: String? = null,
    val revertReason: String? = null
) {

    override fun toString(): String {
        return ("TransactionReceipt{"
                + "transactionHash='"
                + transactionHash
                + '\''
                + ", transactionIndex='"
                + transactionIndex
                + '\''
                + ", blockHash='"
                + blockHash
                + '\''
                + ", blockNumber='"
                + blockNumber
                + '\''
                + ", cumulativeGasUsed='"
                + cumulativeGasUsed
                + '\''
                + ", gasUsed='"
                + gasUsed
                + '\''
                + ", contractAddress='"
                + contractAddress
                + '\''
                + ", root='"
                + root
                + '\''
                + ", status='"
                + status
                + '\''
                + ", from='"
                + from
                + '\''
                + ", to='"
                + to
                + '\''
                + ", logs="
                + logs
                + ", logsBloom='"
                + logsBloom
                + '\''
                + ", revertReason='"
                + revertReason
                + '\''
                + '}')
    }
}
