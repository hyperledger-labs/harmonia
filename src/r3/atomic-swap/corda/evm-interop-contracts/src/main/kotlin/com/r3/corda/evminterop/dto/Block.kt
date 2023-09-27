package com.r3.corda.evminterop.dto

import net.corda.core.serialization.CordaSerializable
import java.math.BigInteger

/**
 * A Corda serializable version of the Web3j's Block that acts as a DTO and allows flows to return the
 * ethereum Block object using Corda serialization.
 */
@CordaSerializable
public class Block(
    val number: BigInteger,
    val hash: String,
    val parentHash: String,
    val nonce: BigInteger,
    val sha3Uncles: String,
    val logsBloom: String,
    val transactionsRoot: String,
    val stateRoot: String,
    val receiptsRoot: String,
    //val author: String,
    val miner: String,
    val mixHash: String,
    val difficulty: BigInteger,
    val totalDifficulty: BigInteger,
    val extraData: String,
    val size: BigInteger,
    val gasLimit: BigInteger,
    val gasUsed: BigInteger,
    val timestamp: BigInteger,
    val transactions: List<Transaction>,
    val uncles: List<String>,
    //val sealFields: List<String>,
    val baseFeePerGas: BigInteger
 ) {

}
