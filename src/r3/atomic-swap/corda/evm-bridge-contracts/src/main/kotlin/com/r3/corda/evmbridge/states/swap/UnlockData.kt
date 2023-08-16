package com.r3.corda.evmbridge.states.swap

import com.r3.corda.cno.evmbridge.dto.TransactionReceipt
import com.r3.corda.interop.evm.common.trie.SimpleKeyValueStore
import net.corda.core.crypto.DigitalSignature
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class UnlockData(
    val merkleProof: SimpleKeyValueStore,
    val validatorSignatures: List<DigitalSignature.WithKey>,
    val receiptsRootHash: String,
    val transactionReceipt: TransactionReceipt
)