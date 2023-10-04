package com.r3.corda.evminterop

import net.corda.core.crypto.SecureHash
import org.web3j.abi.DefaultFunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Bytes32
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Hash
import org.web3j.utils.Numeric
import java.math.BigInteger

data class SwapVaultEventEncoder(
    private val protocolAddress: String,
    private val commitmentHash: ByteArray
) {
    companion object {
        fun create(
            chainId: BigInteger,
            protocolAddress: String,
            owner: String,
            recipient: String,
            amount: BigInteger,
            tokenId: BigInteger,
            tokenAddress: String,
            signaturesThreshold: BigInteger
        ) : SwapVaultEventEncoder {
            return SwapVaultEventEncoder(
                protocolAddress,
                commitmentHash(
                    chainId,
                    owner,
                    recipient,
                    amount,
                    tokenId,
                    tokenAddress,
                    signaturesThreshold
                )
            )
        }

        private fun commitmentHash(
            chainId: BigInteger,
            owner: String,
            recipient: String,
            amount: BigInteger,
            tokenId: BigInteger,
            tokenAddress: String,
            signaturesThreshold: BigInteger
        ): ByteArray {
            val parameters = listOf<Type<*>>(
                Uint256(chainId),
                Address(owner),
                Address(recipient),
                Uint256(amount),
                Uint256(tokenId),
                Address(tokenAddress),
                Uint256(signaturesThreshold)
            )

            // Encode parameters using the DefaultFunctionEncoder
            val encodedParams = DefaultFunctionEncoder().encodeParameters(parameters)

            val bytes = Numeric.hexStringToByteArray(encodedParams)

            val hash = Hash.sha3(bytes)

            return hash
        }
    }

    public fun commitEvent(transactionId: SecureHash) = commitEvent(transactionId, Bytes32(commitmentHash))
    public fun transferEvent(transactionId: SecureHash) = commitEvent(transactionId, Bytes32(commitmentHash))
    public fun revertEvent(transactionId: SecureHash) = commitEvent(transactionId, Bytes32(commitmentHash))

    private fun commitEvent(transactionId: SecureHash, commitmentHash: Bytes32): EncodedEvent {
        return DefaultEventEncoder.encodeEvent(
            protocolAddress,
            "Commit(string,bytes32)",
            Indexed(transactionId.toHexString()),
            commitmentHash
        )
    }

    private fun transferEvent(transactionId: SecureHash, commitmentHash: Bytes32): EncodedEvent {
        return DefaultEventEncoder.encodeEvent(
            protocolAddress,
            "Transfer(string,bytes32)",
            Indexed(transactionId.toHexString()),
            commitmentHash
        )
    }

    private fun revertEvent(transactionId: SecureHash, commitmentHash: Bytes32): EncodedEvent {
        return DefaultEventEncoder.encodeEvent(
            protocolAddress,
            "Revert(string,bytes32)",
            Indexed(transactionId.toHexString()),
            commitmentHash
        )
    }
}