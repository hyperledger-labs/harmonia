package com.r3.corda.evminterop.states.swap

import com.r3.corda.evminterop.dto.TransactionReceipt
import com.r3.corda.interop.evm.common.trie.SimpleKeyValueStore
import net.corda.core.crypto.DigitalSignature
import net.corda.core.serialization.CordaSerializable

/**
 * Defines the data structure containing the parameters required to unlock a Corda transaction using an EVM event proofs
 */
@CordaSerializable
data class UnlockData(
    val merkleProof: SimpleKeyValueStore,
    val validatorSignatures: List<DigitalSignature.WithKey>,
    val receiptsRootHash: String,
    val transactionReceipt: TransactionReceipt
)