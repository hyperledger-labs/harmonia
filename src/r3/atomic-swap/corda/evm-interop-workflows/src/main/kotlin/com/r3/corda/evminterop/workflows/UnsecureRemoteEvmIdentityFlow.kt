package com.r3.corda.evminterop.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.services.BridgeIdentity
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import java.net.URI

/**
 * UnsecureRemoteEvmIdentityFlow associate an EVM identity to a Corda Identity and EVM RPC Endpoint
 * Flows using the EVM service run by this Corda Identity will sign with the specified private key
 * NOTE: This is a simplistic implementation where the private key is passed in plain text as a
 * parameter. A safer implementation is recommended using HSM devices or protocols for offline
 * signing.
 */
@InitiatingFlow
@StartableByRPC
class UnsecureRemoteEvmIdentityFlow(
    private val ethereumPrivateKey: String,
    private val jsonRpcEndpoint: String,
    private val chainId: Long = -1,
    private val protocolAddress: String,
    private val deployerAddress: String
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {

        val identity = BridgeIdentity(
            privateKey = ethereumPrivateKey,
            rpcEndpoint = URI(jsonRpcEndpoint),
            chainId = chainId,
            protocolAddress = protocolAddress,
            deployerAddress = deployerAddress
        )

        identity.authorize(this, ourIdentity.owningKey)
    }
}
