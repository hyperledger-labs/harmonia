package com.r3.corda.evminterop

import com.r3.corda.evminterop.dto.TransactionReceipt
import com.r3.corda.evminterop.dto.TransactionReceiptLog
import net.corda.core.serialization.CordaSerializable
import org.web3j.abi.DefaultFunctionEncoder
import org.web3j.abi.TypeEncoder
import org.web3j.abi.datatypes.*
import org.web3j.abi.datatypes.generated.Int256
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.crypto.Hash
import org.web3j.utils.Numeric
import java.io.Serializable
import java.math.BigInteger

data class Indexed<T>(val indexedValue: T)

object DefaultEventEncoder {

    private val whitespaceRegex = Regex("\\s+")

    fun encodeEvent(contractAddress: String, eventSignature: String, vararg params: Any): EncodedEvent {
        val paramTypesString = eventSignature.substringAfter('(').substringBefore(')')
        val paramTypes = paramTypesString.split(',').map { it.trim() }

        fun toWeb3jType(value: Any, type: String): Pair<Type<out Serializable>, Boolean> {
            val isIndexed = value is Indexed<*>
            val unwrappedValue = if (isIndexed) (value as Indexed<*>).indexedValue else value

            return Pair(when (type) {
                "string" -> Utf8String(unwrappedValue as String)
                "uint256" -> Uint256(unwrappedValue as BigInteger)
                "uint8" -> Uint8(unwrappedValue as BigInteger)
                "int256" -> Int256(unwrappedValue as BigInteger)
                "address" -> Address(unwrappedValue as String)
                "bool" -> Bool(unwrappedValue as Boolean)
                "bytes" -> DynamicBytes(unwrappedValue as ByteArray)
                else -> throw IllegalArgumentException("Unsupported type: $type")
            }, isIndexed)
        }

        val web3jParamsWithIndexedInfo = params.zip(paramTypes).map { (value, type) -> toWeb3jType(value, type) }

        val indexedParams = web3jParamsWithIndexedInfo.filter { it.second }.map { it.first }
        val nonIndexedParams = web3jParamsWithIndexedInfo.filterNot { it.second }.map { it.first }

        val topic0 = Hash.sha3String(whitespaceRegex.replace(eventSignature, ""))
        val topics = listOf(topic0) + indexedParams.map { Numeric.prependHexPrefix(TypeEncoder.encode(it)) }
        val data = Numeric.prependHexPrefix(DefaultFunctionEncoder().encodeParameters(nonIndexedParams))

        return EncodedEvent(contractAddress, topics, data)
    }
}

@CordaSerializable
data class EncodedEvent(
    val address: String,
    val topics: List<String>,
    val data: String
) {
    companion object {
        val defaultLog = TransactionReceiptLog()
    }

    data class Log(val found: Boolean, val log: TransactionReceiptLog)

    fun isFoundIn(receipt: TransactionReceipt): Boolean {
        // NOTE: while generally speaking there may be multiple events with the same parameters, our use cases expects
        //       it to be unique due to the presence of Draft Transaction ID and the rules of the contract that does not
        //       allow its reuse
        return Numeric.toBigInt(receipt.status) != BigInteger.ZERO && receipt.logs?.count {
                    !it.removed &&
                    address.equals(it.address, ignoreCase = true) &&
                    areTopicsEqual(it.topics, topics) &&
                    data.equals(it.data, ignoreCase = true)
                } == 1
    }

    fun findIn(receipt: TransactionReceipt): Log {
        var log: TransactionReceiptLog? = null

        if(Numeric.toBigInt(receipt.status) != BigInteger.ZERO) {
            log = receipt.logs?.singleOrNull {
                !it.removed &&
                address.equals(it.address, ignoreCase = true) &&
                areTopicsEqual(it.topics, topics) &&
                data.equals(it.data, ignoreCase = true)
            }
        }

        return Log(log != null, log ?: defaultLog)
    }

    fun findInByHash(receipt: TransactionReceipt) : Log {
        val hash = eventHash()
        val log = receipt.logs!!.singleOrNull { hashReceiptLog(it) == hash }
        return Log(log != null, log ?: defaultLog)
    }

    fun eventHash() : String {
        return Hash.sha3String((address + topics.joinToString("") + data).toLowerCase())
    }

    private fun hashReceiptLog(log: TransactionReceiptLog) : String {
        return Hash.sha3String((log.address + log.topics!!.joinToString("") + log.data).toLowerCase())
    }

    private fun areTopicsEqual(topics: List<String>?, other: List<String>): Boolean {
        if (topics == null || topics.size != other.size) return false

        for (i in topics.indices) {
            if (!other[i].equals(topics[i], ignoreCase = true)) {
                return false
            }
        }

        return true
    }
}

