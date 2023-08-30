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
 * Sets amount of ERC20 tokens as the allowance of spenderAddress over the owner's (signerAddress) tokens.
 *
 * @property tokenAddress the address of the ERC20 contract representing the token for which the allowance is set.
 * @property spenderAddress the address of the ERC20 recipient whose receiving the spend allowance.
 * @property amount the allowance amount.
 * @return the ERC20's total supply.
 */
@StartableByRPC
@InitiatingFlow
class Erc20TokensApproveFlow(
    private val tokenAddress: String,
    private val spenderAddress: String,
    private val amount: BigInteger
) : FlowLogic<TransactionReceipt>() {

    @Suppress("ClassName")
    companion object {
        object CONNECT_ERC20 : ProgressTracker.Step("Connecting ERC20 contract provider.")
        object QUERYING_ERC20_BALANCE : ProgressTracker.Step("Querying for sufficient ERC20 balance.")
        object SETTING_ERC20_ALLOWANCE : ProgressTracker.Step("Setting the ERC20 tokens allowance for the spender.")


        fun tracker() = ProgressTracker(
            CONNECT_ERC20,
            QUERYING_ERC20_BALANCE,
            SETTING_ERC20_ALLOWANCE
        )

        val log = loggerFor<Erc20TokensApproveFlow>()
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): TransactionReceipt {
        progressTracker.currentStep = CONNECT_ERC20
        val erc20 = evmInterop().erc20Provider(tokenAddress)

        progressTracker.currentStep = QUERYING_ERC20_BALANCE
        val ownerAddress = evmInterop().signerAddress()
        val currentBalance = await(erc20.balanceOf(ownerAddress))
        require(currentBalance >= amount) { "Insufficient balance" }

        progressTracker.currentStep = SETTING_ERC20_ALLOWANCE
        val transactionReceipt = await(erc20.approve(spenderAddress, amount))

        return transactionReceipt
    }
}