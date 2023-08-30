package com.r3.corda.evminterop.states

import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.WireTransaction

@CordaSerializable
data class ValidatedDraftTransferOfOwnership(
    val tx: WireTransaction,
    val controllingNotary: Party,
    val notarySignatureMetadata: SignatureMetadata
) {
    val txHash get() = SignableData(tx.id, notarySignatureMetadata)
    val timeWindow get() = tx.timeWindow!!
}