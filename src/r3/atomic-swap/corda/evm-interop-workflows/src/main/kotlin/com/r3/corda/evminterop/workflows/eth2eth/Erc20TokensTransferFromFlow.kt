package com.r3.corda.evminterop.workflows

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
 * Transfers ERC20 tokens from the specified sender address to the recipient address.
 *
 * @property tokenAddress the address of the ERC20 contract representing the token being transferred.
 * @property receiverAddress the sender address of the transfer.
 * @property receiverAddress the recipient address of the transfer.
 * @property amount the amount to transfer.
 * @return the ethereum transaction receipt of the transfer.
 */
@StartableByRPC
@InitiatingFlow
class Erc20TokensTransferFromFlow(
    private val tokenAddress: String,
    private val senderAddress: String,
    private val receiverAddress: String,
    private val amount: BigInteger
) : FlowLogic<TransactionReceipt>() {

    @Suppress("ClassName")
    companion object {
        object CONNECT_ERC20 : ProgressTracker.Step("Connecting ERC20 contract provider.")
        object QUERYING_ERC20_BALANCE : ProgressTracker.Step("Querying sender address for ERC20 balance.")
        object QUERYING_ERC20_ALLOWANCE : ProgressTracker.Step("Querying ERC20 allowance for sender/recipient pair.")
        object TRANSFER_ERC20_AMOUNT : ProgressTracker.Step("Transferring ERC20 amount to receiver address.")

        fun tracker() = ProgressTracker(
            CONNECT_ERC20,
            QUERYING_ERC20_ALLOWANCE,
            QUERYING_ERC20_BALANCE,
            TRANSFER_ERC20_AMOUNT
        )

        val log = loggerFor<Erc20TokensTransferFromFlow>()
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): TransactionReceipt {
        progressTracker.currentStep = CONNECT_ERC20
        val erc20 = evmInterop().erc20Provider(tokenAddress)

        progressTracker.currentStep = QUERYING_ERC20_BALANCE
        val balance = await(erc20.balanceOf(senderAddress))
        require(balance >= amount) { "Owner has insufficient balance" }

        progressTracker.currentStep = QUERYING_ERC20_ALLOWANCE
        val signerAddress = evmInterop().signerAddress()
        val allowance = await(erc20.allowance(senderAddress, signerAddress))
        require(allowance >= amount) { "Allowance insufficient for the requested amount" }

        progressTracker.currentStep = TRANSFER_ERC20_AMOUNT
        val transactionReceipt = await(erc20.transferFrom(senderAddress, receiverAddress, amount))

        return transactionReceipt
    }
}