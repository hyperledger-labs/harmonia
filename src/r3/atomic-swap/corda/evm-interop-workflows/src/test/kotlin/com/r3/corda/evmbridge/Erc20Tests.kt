package com.r3.corda.evminterop

import com.r3.corda.evminterop.internal.TestNetSetup
import com.r3.corda.evminterop.workflows.*
import net.corda.core.utilities.getOrThrow
import org.junit.Test
import java.math.BigInteger
import kotlin.test.assertEquals

class Erc20Tests : TestNetSetup() {

    @Test
    fun `can read ERC20 token metadata`() {
        val metaData = alice.startFlow(
            GetTokenMetadataByAddressFlow(goldTokenDeployAddress)
        ).getOrThrow()

        assertEquals("Gold Tethered", metaData.name)
        assertEquals("GLDT", metaData.symbol)
        assertEquals(18.toBigInteger(), metaData.decimals)
        assertEquals("0x5FbDB2315678afecb367f032d93F642f64180aa3", metaData.address)
    }

    @Test
    fun `can read ERC20 token balance`() {
        val balance = alice.startFlow(
            Erc20TokensBalanceFlow(goldTokenDeployAddress, aliceAddress)
        ).getOrThrow()

        assert(balance > BigInteger.ZERO)
    }

    @Test
    fun `can read ERC20 total supply`() {
        val supply = alice.startFlow(
            Erc20TokensTotalSupplyFlow(goldTokenDeployAddress)
        ).getOrThrow()

        assert(supply > BigInteger.ZERO)
    }

    @Test
    fun `can query ERC20 allowance`() {
        val allowance = alice.startFlow(
            Erc20TokensAllowanceFlow(goldTokenDeployAddress, aliceAddress, bobAddress)
        ).getOrThrow()

        assertEquals(BigInteger.ZERO, allowance)
    }

    @Test
    fun `can transfer ERC20 tokens from Alice to Bob`() {
        val aliceBalanceBefore = alice.startFlow(
            Erc20TokensBalanceFlow(goldTokenDeployAddress, aliceAddress)
        ).getOrThrow()
        val bobBalanceBefore = alice.startFlow(
            Erc20TokensBalanceFlow(goldTokenDeployAddress, bobAddress)
        ).getOrThrow()

        val transactionReceipt = alice.startFlow(
            Erc20TransferFlow(goldTokenDeployAddress, bobAddress, 1.toBigInteger())
        ).getOrThrow()

        val aliceBalanceAfter = alice.startFlow(
            Erc20TokensBalanceFlow(goldTokenDeployAddress, aliceAddress)
        ).getOrThrow()
        val bobBalanceAfter = alice.startFlow(
            Erc20TokensBalanceFlow(goldTokenDeployAddress, bobAddress)
        ).getOrThrow()

        assertEquals(1.toBigInteger(), aliceBalanceBefore - aliceBalanceAfter)
        assertEquals(1.toBigInteger(), bobBalanceAfter - bobBalanceBefore)
    }

    @Test
    fun `Charlie can transfer Alice ERC20 tokens to Bob`() {
        // Alice approves Charlie to spend 1 token
        val approveTxReceipt = alice.startFlow(
            Erc20TokensApproveFlow(goldTokenDeployAddress, charlieAddress, 1.toBigInteger())
        ).getOrThrow()

        // Charlie check his allowance from Alice's balance
        val allowance = charlie.startFlow(
            Erc20TokensAllowanceFlow(goldTokenDeployAddress, aliceAddress, charlieAddress)
        ).getOrThrow()

        assertEquals(1.toBigInteger(), allowance)

        val aliceBalanceBefore = alice.startFlow(
            Erc20TokensBalanceFlow(goldTokenDeployAddress, aliceAddress)
        ).getOrThrow()
        val bobBalanceBefore = alice.startFlow(
            Erc20TokensBalanceFlow(goldTokenDeployAddress, bobAddress)
        ).getOrThrow()

        // Charlie transfer 1 token from Alice to Bob
        val transferTxReceipt = charlie.startFlow(
            Erc20TokensTransferFromFlow(goldTokenDeployAddress, aliceAddress, bobAddress, 1.toBigInteger())
        ).getOrThrow()

        val aliceBalanceAfter = alice.startFlow(Erc20TokensBalanceFlow(goldTokenDeployAddress, aliceAddress)).getOrThrow()
        val bobBalanceAfter = alice.startFlow(Erc20TokensBalanceFlow(goldTokenDeployAddress, bobAddress)).getOrThrow()

        assertEquals(1.toBigInteger(), aliceBalanceBefore - aliceBalanceAfter)
        assertEquals(1.toBigInteger(), bobBalanceAfter - bobBalanceBefore)
    }
}