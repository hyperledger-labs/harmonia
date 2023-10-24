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

    fun authorize(flowLogic: FlowLogic<*>, authorizedId: PublicKey)

    fun signMessage(rawTransaction: RawTransaction, chainId: Long) : ByteArray

    fun getAddress() : String

    fun signData(data: ByteArray) : ByteArray

    fun dispose()
}
