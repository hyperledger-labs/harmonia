package com.r3.corda.evminterop.workflows.token

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.dto.TokenMetadata
import com.r3.corda.evminterop.services.evmInterop
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.ProgressTracker

/**
 * Queries the ERC20 network provider for metadata about a given ERC20 contract by its network deployment address.
 *
 * @property address the deployment address of the ERC20 contract.
 * @return the [TokenMetadata] info for the given ERC20 contract, which includes token's name, symbol, and decimal.
 */
@StartableByRPC
@InitiatingFlow
class GetTokenMetadataByAddressFlow(
    private val address: String
) : FlowLogic<TokenMetadata>() {

    @Suppress("ClassName")
    companion object {
        object CONNECT_ERC20 : ProgressTracker.Step("Connecting ERC20 contract provider.")
        object QUERYING_ERC20_NAME : ProgressTracker.Step("Querying ERC20 contract name.")
        object QUERYING_ERC20_SYMBOL : ProgressTracker.Step("Querying ERC20 contract symbol.")
        object QUERYING_ERC20_DECIMALS : ProgressTracker.Step("Querying ERC20 contract decimals.")

        fun tracker() = ProgressTracker(
            CONNECT_ERC20,
            QUERYING_ERC20_NAME,
            QUERYING_ERC20_SYMBOL,
            QUERYING_ERC20_DECIMALS
        )
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): TokenMetadata {

        progressTracker.currentStep = CONNECT_ERC20
        val erc20 = evmInterop().erc20Provider(address)

        progressTracker.currentStep = QUERYING_ERC20_NAME
        val name = await(erc20.name())

        progressTracker.currentStep = QUERYING_ERC20_SYMBOL
        val symbol = await(erc20.symbol())

        progressTracker.currentStep = QUERYING_ERC20_DECIMALS
        val decimals = await(erc20.decimals())

        return TokenMetadata(address, name, symbol, decimals)
    }
}