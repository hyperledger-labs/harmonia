package com.r3.corda.evminterop.services.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.services.IdentityServiceProvider
import com.r3.corda.evminterop.services.RemoteEVMIdentity
import net.corda.core.flows.FlowLogic
import java.net.URI
import java.security.PublicKey

abstract class RemoteEVMIdentityImpl(
    override val rpcEndpoint: URI,
    override val chainId: Long,
    override val protocolAddress: String,
    override val deployerAddress: String
    ) : RemoteEVMIdentity {

    @Suspendable
    override fun authorize(flowLogic: FlowLogic<*>, authorizedId: PublicKey) {
        flowLogic.serviceHub.cordaService(IdentityServiceProvider::class.java).authorize(this, authorizedId)
    }
}