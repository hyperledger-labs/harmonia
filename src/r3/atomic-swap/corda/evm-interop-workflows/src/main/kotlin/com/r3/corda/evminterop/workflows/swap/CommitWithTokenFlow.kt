package com.r3.corda.evminterop.workflows.swap

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.dto.TransactionReceipt
import com.r3.corda.evminterop.services.evmInterop
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import java.math.BigInteger

/**
 *
 */
@StartableByRPC
@InitiatingFlow
class CommitWithTokenFlow(
        private val transactionId: SecureHash,
        private val tokenAddress: String,
        private val tokenId: BigInteger,
        private val amount: BigInteger,
        private val recipient: String,
        private val signaturesThreshold: BigInteger
) : FlowLogic<TransactionReceipt>() {

    constructor(
        transactionId: SecureHash,
        tokenAddress: String,
        amount: BigInteger,
        recipient: String,
        signaturesThreshold: BigInteger
    ) : this(transactionId, tokenAddress, BigInteger.ZERO, amount, recipient, signaturesThreshold)

    @Suspendable
    override fun call(): TransactionReceipt {

        val swapProvider = evmInterop().swapProvider()
        val ercProvider = evmInterop().erc20Provider(tokenAddress)

        val txReceipt1 = await(ercProvider.approve(swapProvider.contractAddress, amount))

        val txReceipt2 = await(
                if(tokenId == BigInteger.ZERO) {
                    swapProvider.commitWithToken(
                            transactionId.toString(),
                            tokenAddress,
                            amount,
                            recipient,
                            signaturesThreshold
                    )
                } else {
                    swapProvider.commitWithToken(
                            transactionId.toString(),
                            tokenAddress,
                            amount,
                            recipient,
                            signaturesThreshold
                    )
                })


        return txReceipt2
    }
}
