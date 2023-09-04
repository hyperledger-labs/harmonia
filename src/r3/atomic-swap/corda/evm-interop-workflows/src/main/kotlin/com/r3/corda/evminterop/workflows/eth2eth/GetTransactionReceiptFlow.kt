package com.r3.corda.evminterop.workflows.eth2eth

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.services.evmInterop
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.loggerFor

/**
 * Get a transaction receipt by its transaction hash
 *
 * @property hash the hash of the transaction
 * @return the ethereum transaction receipt of the transfer.
 */
@StartableByRPC
@InitiatingFlow
class GetTransactionReceiptFlow(
    private val hash: String
) : FlowLogic<com.r3.corda.evminterop.dto.TransactionReceipt>() {

    @Suppress("ClassName")
    companion object {
        object CONNECT_WEB3 : ProgressTracker.Step("Connecting WEB3 API provider.")
        object QUERY_TRANSACTION_RECEIPT : ProgressTracker.Step("Querying transaction receipt.")

        fun tracker() = ProgressTracker(
            CONNECT_WEB3,
            QUERY_TRANSACTION_RECEIPT
        )

        val log = loggerFor<GetTransactionReceiptFlow>()
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): com.r3.corda.evminterop.dto.TransactionReceipt {
        progressTracker.currentStep = CONNECT_WEB3
        val web3 = evmInterop().web3Provider()

        progressTracker.currentStep = QUERY_TRANSACTION_RECEIPT
        val receipt = await(web3.getTransactionReceiptByHash(hash))

        return receipt
    }
}

