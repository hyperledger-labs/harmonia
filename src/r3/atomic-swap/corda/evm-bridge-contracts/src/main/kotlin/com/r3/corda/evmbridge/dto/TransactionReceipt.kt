package com.r3.corda.cno.evmbridge.dto

import net.corda.core.serialization.CordaSerializable
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpList
import org.web3j.rlp.RlpString
import org.web3j.utils.Numeric
import java.math.BigInteger

/**
 * A Corda serializable version of the Web3j's TransactionReceipt that acts as a DTO and allows flows to return the
 * ethereum transaction receipt object using Corda serialization.
 */
@CordaSerializable
data class
TransactionReceipt(
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

fun TransactionReceipt.encoded() : ByteArray {
    fun serializeLog(log: TransactionReceiptLog): RlpList {
        val address = Numeric.hexStringToByteArray(log.address)
        val topics = log.topics?.map { topic -> Numeric.hexStringToByteArray(topic) } ?: listOf()

        require(address.size == 20) { "Invalid contract address size (${address.size})" }
        require(topics.isNotEmpty() && topics.all { it.size == 32}) { "Invalid topics length or size" }

        return RlpList(listOf(
            RlpString.create(address),
            RlpList(topics.map { topic -> RlpString.create(topic) }),
            RlpString.create(Numeric.hexStringToByteArray(log.data))
        ))
    }

    return RlpEncoder.encode(RlpList(listOf(
        RlpString.create(Numeric.toBigInt(this.status)),
        RlpString.create(Numeric.toBigInt(this.cumulativeGasUsed)),
        RlpString.create(Numeric.hexStringToByteArray(this.logsBloom)),
        RlpList(this.logs?.map { serializeLog(it) } ?: listOf())
    )))
}


