package com.r3.corda.evminterop.workflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.services.evmInterop
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.loggerFor
import java.math.BigInteger

/**
 * Get all transaction receipts from a block
 *
 * @property blockNumber the number of the block
 * @return the ethereum transaction receipts of a block.
 */
@StartableByRPC
@InitiatingFlow
class GetBlockReceiptsFlow(
    private val blockNumber: BigInteger
) : FlowLogic<List<com.r3.corda.evminterop.dto.TransactionReceipt>>() {

    @Suppress("ClassName")
    companion object {
        object CONNECT_WEB3 : ProgressTracker.Step("Connecting WEB3 API provider.")
        object QUERY_BLOCK_RECEIPTS : ProgressTracker.Step("Querying block receipts.")

        fun tracker() = ProgressTracker(
            CONNECT_WEB3,
            QUERY_BLOCK_RECEIPTS
        )

        val log = loggerFor<GetBlockReceiptsFlow>()
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): List<com.r3.corda.evminterop.dto.TransactionReceipt> {
        progressTracker.currentStep = CONNECT_WEB3
        val web3 = evmInterop().web3Provider()

        progressTracker.currentStep = QUERY_BLOCK_RECEIPTS
        // NOTE: getBlockReceipts depends on Web3j 4.10.1 and clients supporting eth_getBlockReceipts, therefore we're
        //       temporarily querying receipts by iterating through block transactions, one by one.
        //val receipts = await(web3.getBlockReceipts(blockNumber))
        val block = await(web3.getBlockByNumber(blockNumber, true))
        val receipts = block.transactions.map {
            await(web3.getTransactionReceiptByHash(it.hash))
        }

        return receipts
    }
}

