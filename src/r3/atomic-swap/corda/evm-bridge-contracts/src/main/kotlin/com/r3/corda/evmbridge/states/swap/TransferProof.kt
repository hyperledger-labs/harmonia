package com.r3.corda.evmbridge.states.swap

import net.corda.core.crypto.DigitalSignature
import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class TransferProof(
    val merkleProof: ByteArray,
    val validatorSignatures: List<DigitalSignature.WithKey>,
    val transactionRootHash: SecureHash,
    val transferEvent: TransferEvent
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TransferProof

        if (!merkleProof.contentEquals(other.merkleProof)) return false
        if (validatorSignatures != other.validatorSignatures) return false
        if (transactionRootHash != other.transactionRootHash) return false
        if (transferEvent != other.transferEvent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = merkleProof.contentHashCode()
        result = 31 * result + validatorSignatures.hashCode()
        result = 31 * result + transactionRootHash.hashCode()
        result = 31 * result + transferEvent.hashCode()
        return result
    }
}