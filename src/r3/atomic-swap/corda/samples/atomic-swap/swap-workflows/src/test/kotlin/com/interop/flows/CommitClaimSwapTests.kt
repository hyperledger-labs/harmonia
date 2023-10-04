package com.interop.flows

import com.interop.flows.internal.TestNetSetup
import com.r3.corda.evminterop.DefaultEventEncoder
import com.r3.corda.evminterop.EncodedEvent
import com.r3.corda.evminterop.Indexed
import com.r3.corda.evminterop.dto.TransactionReceipt
import com.r3.corda.evminterop.workflows.IssueGenericAssetFlow
import com.r3.corda.evminterop.workflows.swap.CommitWithTokenFlow
import com.r3.corda.evminterop.workflows.swap.CommitmentHash
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.utilities.getOrThrow
import org.junit.Test
import org.web3j.abi.DefaultFunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.generated.Bytes32
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Hash
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*

class CommitClaimSwapTests : TestNetSetup() {

    private val amount = 1.toBigInteger()


    fun commitmentHash(
            chainId: BigInteger,
            owner: String,
            recipient: String,
            amount: BigInteger,
            tokenId: BigInteger,
            tokenAddress: String,
            signaturesThreshold: BigInteger
    ): Bytes32 {
        val parameters = listOf<org.web3j.abi.datatypes.Type<*>>(
            Uint256(chainId),
            Address(owner),
            Address(recipient),
            Uint256(amount),
            Uint256(tokenId),
            Address(tokenAddress),
            Uint256(signaturesThreshold)
        )

        // Encode parameters using the DefaultFunctionEncoder
        val encodedParams = DefaultFunctionEncoder().encodeParameters(parameters)

        val bytes = Numeric.hexStringToByteArray(encodedParams)

        val hash = Hash.sha3(bytes)

        return Bytes32(hash)
    }

    private fun commitEvent(transactionId: SecureHash, commitmentHash: Bytes32) {
        DefaultEventEncoder.encodeEvent(
            protocolAddress,
            "Commit(string,bytes32)",
            Indexed(transactionId.toHexString()),
            commitmentHash
        )
    }

    private fun transferEvent(transactionId: SecureHash, commitmentHash: Bytes32): EncodedEvent {
        return DefaultEventEncoder.encodeEvent(
                protocolAddress,
                "Transfer(string,bytes32)",
                Indexed(transactionId.toHexString()),
                commitmentHash
        )
    }

    private fun revertEvent(transactionId: SecureHash, commitmentHash: Bytes32): EncodedEvent {
        return DefaultEventEncoder.encodeEvent(
                protocolAddress,
                "Revert(string,bytes32)",
                Indexed(transactionId.toHexString()),
                commitmentHash
        )
    }

    @Test
    fun `can unlock corda asset by asynchronous collection of block signatures`() {
        val assetName = UUID.randomUUID().toString()

        // Create Corda asset owned by Bob
        val assetTx : StateRef = await(bob.startFlow(IssueGenericAssetFlow(assetName)))

        val transferEventEncoded = transferEvent(assetTx.txhash, commitmentHash(
                BigInteger.valueOf(1337),
                aliceAddress,
                bobAddress,
                amount,
                BigInteger.ZERO,
                goldTokenDeployAddress,
                BigInteger.ONE
        ))

        val revertEventEncoded = revertEvent(assetTx.txhash, commitmentHash(
                BigInteger.valueOf(1337),
                aliceAddress,
                bobAddress,
                amount,
                BigInteger.ZERO,
                goldTokenDeployAddress,
                BigInteger.ONE
        ))

        val draftTxHashz = await(bob.startFlow(DraftAssetSwapFlowNew(
            assetTx.txhash,
            assetTx.index,
            alice.toParty(),
            alice.services.networkMapCache.notaryIdentities.first(),
            listOf(charlie.toParty() as AbstractParty, bob.toParty() as AbstractParty),
            2,
            transferEventEncoded,
            revertEventEncoded
        )))

        val draftTxHash = await(bob.startFlow(DraftAssetSwapFlow(
            assetTx.txhash,
            assetTx.index,
            alice.toParty(),
            alice.services.networkMapCache.notaryIdentities.first(),
            listOf(charlie.toParty() as AbstractParty, bob.toParty() as AbstractParty),
            2,
            transferEventEncoded,
            revertEventEncoded
        )))

        val stx = await(bob.startFlow(SignDraftTransactionByIDFlow(draftTxHash)))

        val (txReceipt, leafKey, merkleProof) = commitAndClaim(assetTx.txhash, amount, alice, bobAddress, BigInteger.ONE)

        await(bob.startFlow(CollectBlockSignaturesFlow(draftTxHash, txReceipt.blockNumber, false)))

        network?.waitQuiescent()

        val utx = await(bob.startFlow(
            UnlockAssetFlow(
                stx.tx.id,
                txReceipt.blockNumber,
                Numeric.toBigInt(txReceipt.transactionIndex!!)
            )
        ))
    }

    @Test
    fun `can generate matching hash`() {

        val assetName = UUID.randomUUID().toString()

        val assetTx : StateRef = await(bob.startFlow(IssueGenericAssetFlow(assetName)))

        val commitTxReceipt: TransactionReceipt = alice.startFlow(
            CommitWithTokenFlow(assetTx.txhash, goldTokenDeployAddress, amount, bobAddress, BigInteger.ONE)
        ).getOrThrow()

        val commitmentHash1 = alice.startFlow(CommitmentHash(assetTx.txhash)).getOrThrow()

        val commitmentHash2 = commitmentHash(
            BigInteger.valueOf(1337),
            aliceAddress,
            bobAddress,
            amount,
            BigInteger.ZERO,
            goldTokenDeployAddress,
            BigInteger.ONE
        )

        val eq = Numeric.hexStringToByteArray(commitmentHash1)
    }
}

