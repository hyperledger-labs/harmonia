package com.r3.corda.evminterop.workflows.eth2eth

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.dto.Block
import com.r3.corda.evminterop.services.IWeb3
import com.r3.corda.evminterop.services.evmInterop
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.loggerFor
import java.math.BigInteger

/**
 * Get a block by its hash or number
 *
 * @property hash the hash of the transaction
 * @return the ethereum transaction receipt of the transfer.
 */
@StartableByRPC
@InitiatingFlow
@Suspendable
class GetBlockFlow(
    private val hash: String,
    private val includeTransactions: Boolean
) : FlowLogic<Block>() {
    private lateinit var number: BigInteger

    constructor(number: BigInteger, includeTransactions: Boolean) : this("", includeTransactions) {
        this.number = number
    }

    init {
        if(hash.isEmpty()) {
            this.number = BigInteger.ZERO
        }
    }

    @Suppress("ClassName")
    companion object {
        object CONNECT_WEB3 : ProgressTracker.Step("Connecting WEB3 API provider.")
        object QUERY_BLOCK : ProgressTracker.Step("Querying block data.")

        fun tracker() = ProgressTracker(
            CONNECT_WEB3,
            QUERY_BLOCK
        )

        val log = loggerFor<GetBlockFlow>()
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): Block {
        progressTracker.currentStep = CONNECT_WEB3
        val web3 = evmInterop().web3Provider()

        progressTracker.currentStep = QUERY_BLOCK

        val block = if (hash.isEmpty()) {
            await(web3.getBlockByNumber(number, includeTransactions))
        } else {
            await(web3.getBlockByHash(hash, includeTransactions))
        }
        return block
    }
}
