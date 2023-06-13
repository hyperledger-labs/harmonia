package com.r3.corda.cno.evmbridge.dto

import net.corda.core.serialization.CordaSerializable

/**
 * Represents the deploy status of a Solidity contract.
 *
 * @property deployed indicates if there is a contract at the deployAddress or not.
 * @property deployAddress is the contract's current or future deployment address.
 */
@CordaSerializable
data class ContractInfo(
    val deployed: Boolean,
    val deployAddress: String
)