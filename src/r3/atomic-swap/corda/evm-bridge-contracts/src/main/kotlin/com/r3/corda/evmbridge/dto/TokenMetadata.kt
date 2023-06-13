package com.r3.corda.cno.evmbridge.dto

import net.corda.core.serialization.CordaSerializable
import java.math.BigInteger

/**
 * Description of an ERC20 token
 *
 * @property address the ERC20 token deployment address
 * @property name of the ERC20 token
 * @property symbol of the ERC20 token
 * @property decimals of the ERC20 token
 */
@CordaSerializable
data class TokenMetadata(
    val address: String,
    val name: String,
    val symbol: String,
    val decimals: BigInteger
)