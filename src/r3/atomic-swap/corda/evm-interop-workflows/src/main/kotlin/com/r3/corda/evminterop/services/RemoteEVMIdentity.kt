package com.r3.corda.evminterop.services

import net.corda.core.flows.FlowLogic
import org.web3j.crypto.RawTransaction
import java.net.URI
import java.security.PublicKey

interface RemoteEVMIdentity {

    /**
     * [rpcEndpoint] defines the EVM node endpoint to connect to
     */
    val rpcEndpoint: URI

    /**
     * [chainId] defines the blockchain id
     */
    val chainId: Long

    /**
     * [protocolAddress] defines the protocol contract deployment address on the given network
     */
    val protocolAddress: String

    /**
     * [deployerAddress] defines the protocol contract deployment address on the given network
     */
    val deployerAddress: String

    /**
     * Initializes a RemoteEVMIdentity instance
     */
    fun authorize(flowLogic: FlowLogic<*>, authorizedId: PublicKey)

    /**
     * Signs a raw transaction before sending it
     */
    fun signMessage(rawTransaction: RawTransaction, chainId: Long) : ByteArray

    /**
     * Get currently configured identity's address
     */
    fun getAddress() : String

    /**
     * Signs some data using the current EVM identity
     */
    fun signData(data: ByteArray) : ByteArray

    /**
     * Dispose the current instance of RemoteEVMIdentity
     */
    fun dispose()
}
