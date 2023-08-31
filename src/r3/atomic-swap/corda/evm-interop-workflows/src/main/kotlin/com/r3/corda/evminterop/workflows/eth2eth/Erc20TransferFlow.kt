package com.r3.corda.evminterop.workflows.eth2eth

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.dto.TransactionReceipt
import com.r3.corda.evminterop.services.evmInterop
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.loggerFor
import java.math.BigInteger

/**
 * Transfers ERC20 tokens from the current node's wallet to the recipient address.
 *
 * @property tokenAddress the address of the ERC20 contract representing the token being transferred.
 * @property receiverAddress the recipient address of the transfer.
 * @property amount the amount to transfer.
 * @return the ethereum transaction receipt of the transfer.
 */
@StartableByRPC
@InitiatingFlow
class Erc20TransferFlow(
    private val tokenAddress: String,
    private val receiverAddress: String,
    private val amount: BigInteger
) : FlowLogic<TransactionReceipt>() {

    @Suppress("ClassName")
    companion object {
        object CONNECT_ERC20 : ProgressTracker.Step("Connecting ERC20 contract provider.")
        object TRANSFER_ERC20_AMOUNT : ProgressTracker.Step("Transferring ERC20 amount to receiver address.")

        fun tracker() = ProgressTracker(
            CONNECT_ERC20,
            TRANSFER_ERC20_AMOUNT
        )

        val log = loggerFor<Erc20TransferFlow>()
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): TransactionReceipt {
        progressTracker.currentStep = CONNECT_ERC20
        val erc20 = evmInterop().erc20Provider(tokenAddress)

        progressTracker.currentStep = TRANSFER_ERC20_AMOUNT
        val txReceipt = await(erc20.transfer(receiverAddress, amount))

        return txReceipt
    }
}

