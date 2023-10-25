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
class ClaimCommitment(
        private val transactionId: SecureHash
) : FlowLogic<TransactionReceipt>() {

    @Suspendable
    override fun call(): TransactionReceipt {

        val swapProvider = evmInterop().swapProvider()

        return await(swapProvider.claimCommitment(transactionId.toString()))
    }
}

@StartableByRPC
@InitiatingFlow
class ClaimCommitmentWithSignatures(
    private val transactionId: SecureHash,
    private val signatures: List<ByteArray>
) : FlowLogic<TransactionReceipt>() {

    @Suspendable
    override fun call(): TransactionReceipt {

        val swapProvider = evmInterop().swapProvider()

        return await(swapProvider.claimCommitment(transactionId.toString(), signatures))
    }
}
