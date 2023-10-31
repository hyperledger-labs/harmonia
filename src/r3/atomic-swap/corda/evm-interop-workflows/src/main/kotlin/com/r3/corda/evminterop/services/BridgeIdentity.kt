package com.r3.corda.evminterop.services

import com.r3.corda.evminterop.services.internal.RemoteEVMIdentityImpl
import org.web3j.crypto.*
import java.net.URI

data class BridgeIdentity (

    /**
     * [privateKey] defines the signing key and derived account address
     */
    val privateKey: String,

    /**
     * [rpcEndpoint] defines the EVM node endpoint to connect to
     */
    override val rpcEndpoint: URI,

    /**
     * [chainId] of the selected EVM network
     */
    override val chainId: Long = -1,

    /**
     * [protocolAddress] defines the protocol contract deployment address on the given network
     */
    override val protocolAddress: String,

    /**
     * [deployerAddress] defines the protocol contract deployment address on the given network
     */
    override val deployerAddress: String

) : RemoteEVMIdentityImpl(rpcEndpoint, chainId, protocolAddress, deployerAddress) {

    private val credentials = Credentials.create(privateKey)

    override fun signMessage(rawTransaction: RawTransaction, chainId: Long) : ByteArray {
        return if (chainId > -1) {
            TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
        } else {
            TransactionEncoder.signMessage(rawTransaction, credentials)
        }
    }

    override fun getAddress() : String = credentials.address

    override fun signData(data: ByteArray) : ByteArray {
        val ecKeyPair = credentials.ecKeyPair
        val signatureData = Sign.signMessage(data, ecKeyPair)

        val signatureBytes = ByteArray(65)
        System.arraycopy(signatureData.r, 0, signatureBytes, 0, 32)
        System.arraycopy(signatureData.s, 0, signatureBytes, 32, 32)
        System.arraycopy(signatureData.v, 0, signatureBytes, 64, 1)

        val publicKey = Sign.signedMessageToKey(data, signatureData)
        val addressString = Keys.getAddress(publicKey)
        val address = org.web3j.abi.datatypes.Address(addressString)

        return signatureBytes
    }

    override fun dispose() {
    }
}
