package com.interop.flows

import com.interop.flows.internal.TestNetSetup
import com.r3.corda.evminterop.DefaultEventEncoder
import com.r3.corda.evminterop.EncodedEvent
import com.r3.corda.evminterop.Indexed
import com.r3.corda.evminterop.workflows.*
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import org.junit.Test
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Bytes32
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*

class CommitClaimSwapTests : TestNetSetup() {

//    event Commit(string indexed swapId, bytes32 holdHash);
//    event Transfer(string indexed swapId, bytes32 holdHash);
//    event Revert(string indexed swapId, bytes32 holdHash);

    private val amount = 1.toBigInteger()

    fun commitmentHash(
            chainId: BigInteger,
            swapId: String,
            owner: String,
            recipient: String,
            amount: BigInteger,
            tokenId: BigInteger,
            tokenAddress: String,
            signaturesThreshold: BigInteger
    ): Bytes32 {
        val function = Function(
                "none",
                listOf(
                        Uint256(chainId),
                        Utf8String(swapId),
                        Address(owner),
                        Address(recipient),
                        Uint256(amount),
                        Uint256(tokenId),
                        Address(tokenAddress),
                        Uint256(signaturesThreshold)
                ),
                emptyList()
        )

        val encodedFunction = FunctionEncoder.encode(function)

        return Bytes32(Numeric.hexStringToByteArray(encodedFunction.substring(2)))
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
                assetTx.txhash.toHexString(),
                aliceAddress,
                bobAddress,
                amount,
                BigInteger.ZERO,
                goldTokenDeployAddress,
                BigInteger.ONE
        ))

        val revertEventEncoded = revertEvent(assetTx.txhash, commitmentHash(
                BigInteger.valueOf(1337),
                assetTx.txhash.toHexString(),
                aliceAddress,
                bobAddress,
                amount,
                BigInteger.ZERO,
                goldTokenDeployAddress,
                BigInteger.ONE
        ))

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

        val (txReceipt, leafKey, merkleProof) = commitAndClaim(assetTx.txhash, amount, alice, bobAddress)

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
}