package com.r3.corda.evminterop.dto

import net.corda.core.serialization.CordaSerializable
import java.math.BigInteger

/**
 * Description of an ERC20 account balance
 *
 * @property amount of the ERC20 token
 * @property recipient of the ERC20 token balance
 */
@CordaSerializable
data class TokenBalance(val amount: BigInteger, val recipient: String)

/**
 * Description of an ERC20 token at deployment
 *
 * @property tokenName the ERC20 token long name
 * @property tokenSymbol the ERC20 token short name
 * @property balances of the ERC20 token (how the initial supply is distributed)
 */
@CordaSerializable
data class TokenSetup(val tokenName: String, val tokenSymbol: String, val balances: List<TokenBalance>)