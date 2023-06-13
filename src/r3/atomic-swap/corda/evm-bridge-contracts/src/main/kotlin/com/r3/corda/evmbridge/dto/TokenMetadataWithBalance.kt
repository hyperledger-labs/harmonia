package com.r3.corda.cno.evmbridge.dto

import net.corda.core.serialization.CordaSerializable
import java.math.BigInteger

/**
 * Description of an ERC20 token with Ethereum and Corda balance.
 *
 * @property address the ERC20 token deployment address
 * @property name of the ERC20 token
 * @property symbol of the ERC20 token
 * @property decimals of the ERC20 token
 * @property balance of the user for the ERC20 token
 * @property wrappedBalance of the user, in wrapped-tokens, for the ERC20 token
 */
@CordaSerializable
data class TokenMetadataWithBalance(
    val address: String,
    val name: String,
    val symbol: String,
    val decimals: BigInteger,
    val balance: BigInteger,
    val wrappedBalance: BigInteger
)