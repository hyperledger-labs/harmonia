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
 * Query ERC20 tokens for total supply.
 *
 * @property tokenAddress the address of the ERC20 contract representing the token being queried.
 * @return the ERC20's total supply.
 */
@StartableByRPC
@InitiatingFlow
class Erc20TokensTotalSupplyFlow(
    private val tokenAddress: String
) : FlowLogic<BigInteger>() {

    @Suppress("ClassName")
    companion object {
        object CONNECT_ERC20 : ProgressTracker.Step("Connecting ERC20 contract provider.")
        object QUERYING_ERC20_SUPPLY : ProgressTracker.Step("Querying ERC20 total supply.")

        fun tracker() = ProgressTracker(
            CONNECT_ERC20,
            QUERYING_ERC20_SUPPLY
        )

        val log = loggerFor<Erc20TokensTotalSupplyFlow>()
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): BigInteger {
        progressTracker.currentStep = CONNECT_ERC20
        val erc20 = evmInterop().erc20Provider(tokenAddress)

        progressTracker.currentStep = QUERYING_ERC20_SUPPLY
        val totalSupply = await(erc20.totalSupply())

        return totalSupply
    }
}