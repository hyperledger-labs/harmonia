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
 * Query amount of ERC20 tokens a spenderAddress is allowed to spend from the owner's balance.
 *
 * @property tokenAddress the address of the ERC20 contract representing the token for which the allowance is set.
 * @property ownerAddress the address of the ERC20 owner whose allowance is being queried.
 * @property spenderAddress the address of the ERC20 spender the allowance is being queried.
 * @return the ERC20's total supply.
 */
@StartableByRPC
@InitiatingFlow
class Erc20TokensAllowanceFlow(
    private val tokenAddress: String,
    private val ownerAddress: String,
    private val spenderAddress: String
) : FlowLogic<BigInteger>() {

    @Suppress("ClassName")
    companion object {
        object CONNECT_ERC20 : ProgressTracker.Step("Connecting ERC20 contract provider.")
        object QUERYING_ERC20_ALLOWANCE : ProgressTracker.Step("Querying ERC20 allowance.")

        fun tracker() = ProgressTracker(
            CONNECT_ERC20,
            QUERYING_ERC20_ALLOWANCE
        )

        val log = loggerFor<Erc20TokensAllowanceFlow>()
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): BigInteger {
        progressTracker.currentStep = CONNECT_ERC20
        val erc20 = evmInterop().erc20Provider(tokenAddress)

        progressTracker.currentStep = QUERYING_ERC20_ALLOWANCE
        val allowance = await(erc20.allowance(ownerAddress, spenderAddress))

        return allowance
    }
}