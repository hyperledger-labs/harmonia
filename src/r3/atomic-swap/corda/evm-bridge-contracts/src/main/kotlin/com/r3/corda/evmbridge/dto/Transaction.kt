package com.r3.corda.cno.evmbridge.dto

import net.corda.core.serialization.CordaSerializable
import org.web3j.protocol.core.methods.response.AccessListObject
import java.math.BigInteger

/**
 * A Corda serializable version of the Web3j's Transaction that acts as a DTO and allows flows to return the
 * ethereum transaction object using Corda serialization.
 */
@CordaSerializable
public class Transaction(
    val hash: String,
    val nonce: BigInteger,
    val blockHash: String,
    val blockNumber: BigInteger,
    val chainId: Long,
    val transactionIndex: BigInteger,
    val from: String,
    val to: String,
    val value: BigInteger,
    val gasPrice: BigInteger,
    val gas: BigInteger,
    val input: String,
    val raw: String,
    val r: String,
    val s: String,
    val v: Long = 0, // see https://github.com/web3j/web3j/issues/44
    val type: String,
    val maxFeePerGas: BigInteger,
    val maxPriorityFeePerGas: BigInteger
 ) {

}
