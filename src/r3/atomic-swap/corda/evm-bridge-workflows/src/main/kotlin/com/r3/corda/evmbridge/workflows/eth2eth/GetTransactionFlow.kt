package com.r3.corda.evmbridge.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evmbridge.services.evmBridge
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.loggerFor

/**
 * Get a transaction by its hash
 *
 * @property hash the hash of the transaction
 * @return the ethereum transaction receipt of the transfer.
 */
@StartableByRPC
@InitiatingFlow
class GetTransactionFlow(
    private val hash: String
) : FlowLogic<com.r3.corda.cno.evmbridge.dto.Transaction>() {

    @Suppress("ClassName")
    companion object {
        object CONNECT_WEB3 : ProgressTracker.Step("Connecting WEB3 API provider.")
        object QUERY_TRANSACTION : ProgressTracker.Step("Querying transaction data.")

        fun tracker() = ProgressTracker(
            CONNECT_WEB3,
            QUERY_TRANSACTION
        )

        val log = loggerFor<GetTransactionFlow>()
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): com.r3.corda.cno.evmbridge.dto.Transaction {
        progressTracker.currentStep = CONNECT_WEB3
        val web3 = evmBridge().web3Provider()

        progressTracker.currentStep = QUERY_TRANSACTION
        val transaction = await(web3.getTransactionByHash(hash))

        return transaction
    }
}

