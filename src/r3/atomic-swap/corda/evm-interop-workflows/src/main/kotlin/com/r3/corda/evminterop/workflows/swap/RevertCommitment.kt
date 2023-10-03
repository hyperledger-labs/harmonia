package com.r3.corda.evminterop.workflows.swap

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.dto.TransactionReceipt
import com.r3.corda.evminterop.services.evmInterop
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC

/**
 *
 */
@StartableByRPC
@InitiatingFlow
class RevertCommitment(
        private val transactionId: SecureHash
) : FlowLogic<TransactionReceipt>() {

    @Suspendable
    override fun call(): TransactionReceipt {

        val swapProvider = evmInterop().swapProvider()

        val txReceipt =  await(swapProvider.revertCommitment(transactionId.toString()))

        return txReceipt
    }
}