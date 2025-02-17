package com.r3.corda.template.webserver.example

import com.r3.corda.evminterop.workflows.UnsecureRemoteEvmIdentityFlow
import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

private const val CORDA_USER_NAME = "config.rpc.username"
private const val CORDA_USER_PASSWORD = "config.rpc.password"
private const val CORDA_NODE_HOST = "config.rpc.host"
private const val CORDA_RPC_PORT = "config.rpc.port"
private const val EVM_RPC_HOST = "config.evm.rpc.host"
private const val EVM_RPC_PORT = "config.evm.rpc.port"
private const val EVM_RPC_CHAIN_ID = "config.evm.rpc.chainId"
private const val EVM_RPC_PROTOCOL_ADDRESS = "config.evm.rpc.protocolAddress"
private const val EVM_RPC_DEPLOYER_ADDRESS = "config.evm.rpc.deployerAddress"
private const val EVM_PRIVATE_KEY = "config.evm.privateKey"


/**
 * Wraps an RPC connection to a Corda node.
 *
 * The RPC connection is configured using command line arguments.
 *
 * @param host The host of the node we are connecting to.
 * @param rpcPort The RPC port of the node we are connecting to.
 * @param username The username for logging into the RPC client.
 * @param password The password for logging into the RPC client.
 * @property proxy The RPC proxy.
 */
@Component
open class InteropNodeRpcConnection(
    @Value("\${$CORDA_NODE_HOST}") private val host: String,
    @Value("\${$CORDA_USER_NAME}") private val username: String,
    @Value("\${$CORDA_USER_PASSWORD}") private val password: String,
    @Value("\${$CORDA_RPC_PORT}") private val rpcPort: Int,
    @Value("\${$EVM_RPC_HOST}") private val evmRpcHost: String,
    @Value("\${$EVM_RPC_PORT}") private val evmRpcPort: Int,
    @Value("\${$EVM_RPC_CHAIN_ID}") private val evmRpcChainId: Long,
    @Value("\${$EVM_RPC_PROTOCOL_ADDRESS}") private val evmRpcProtocolAddress: String,
    @Value("\${$EVM_RPC_DEPLOYER_ADDRESS}") private val evmRpcDeployerAddress: String,
    @Value("\${$EVM_PRIVATE_KEY}") private val evmPrivateKey: String
) : AutoCloseable {

    lateinit var rpcConnection: CordaRPCConnection
        private set
    lateinit var proxy: CordaRPCOps
        private set

    @PostConstruct
    fun initialiseNodeRPCConnection() {
        val rpcAddress = NetworkHostAndPort(host, rpcPort)
        val rpcClient = CordaRPCClient(rpcAddress)
        val rpcConnection = rpcClient.start(username, password)
        proxy = rpcConnection.proxy

        proxy.startFlow(
            ::UnsecureRemoteEvmIdentityFlow,
            evmPrivateKey,
            "http://$evmRpcHost:$evmRpcPort",
            evmRpcChainId,
            evmRpcProtocolAddress,
            evmRpcDeployerAddress
        )
    }

    @PreDestroy
    override fun close() {
        rpcConnection.notifyServerAndClose()
    }
}