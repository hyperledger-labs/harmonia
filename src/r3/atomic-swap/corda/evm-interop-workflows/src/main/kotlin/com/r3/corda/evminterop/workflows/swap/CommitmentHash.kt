package com.r3.corda.evminterop.workflows.swap

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.services.evmInterop
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import org.web3j.utils.Numeric

/**
 *
 */
@StartableByRPC
@InitiatingFlow
class CommitmentHash(
        private val transactionId: SecureHash
) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {

        val swapProvider = evmInterop().swapProvider()

        val hash =  await(swapProvider.commitmentHash(transactionId.toString()))

        return Numeric.toHexString(hash)
    }
}