package com.r3.corda.evmbridge.services

import com.r3.corda.evmbridge.services.internal.RemoteEVMIdentityImpl
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
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

    override fun dispose() {
    }
}