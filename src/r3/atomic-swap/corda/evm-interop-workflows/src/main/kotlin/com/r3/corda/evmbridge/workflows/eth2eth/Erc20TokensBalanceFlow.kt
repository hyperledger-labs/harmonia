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
 * Query an address for ERC20 tokens balance.
 *
 * @property tokenAddress the address of the ERC20 contract representing the token for which the balance is queried.
 * @property holderAddress the address of the ERC20 holder whose balance is being queried for.
 * @return the ERC20's total supply.
 */
@StartableByRPC
@InitiatingFlow
class Erc20TokensBalanceFlow(
    private val tokenAddress: String,
    private val holderAddress: String
) : FlowLogic<BigInteger>() {

    @Suppress("ClassName")
    companion object {
        object CONNECT_ERC20 : ProgressTracker.Step("Connecting ERC20 contract provider.")
        object QUERYING_ERC20_BALANCE : ProgressTracker.Step("Querying address for ERC20 balance.")

        fun tracker() = ProgressTracker(
            CONNECT_ERC20,
            QUERYING_ERC20_BALANCE
        )

        val log = loggerFor<Erc20TokensBalanceFlow>()
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): BigInteger {
        progressTracker.currentStep = CONNECT_ERC20
        val erc20 = evmInterop().erc20Provider(tokenAddress)

        progressTracker.currentStep = QUERYING_ERC20_BALANCE
        val balance = await(erc20.balanceOf(holderAddress))

        return balance
    }
}