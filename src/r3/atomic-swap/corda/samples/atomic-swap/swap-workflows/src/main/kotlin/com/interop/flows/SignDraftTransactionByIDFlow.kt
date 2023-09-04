package com.interop.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.services.swap.DraftTxService
import com.r3.corda.evminterop.workflows.swap.SignDraftTransactionFlow
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction

/**
 * Initiating flow which takes a draft transaction ID and attempts to sign and notarize it.
 */
@StartableByRPC
@InitiatingFlow
class SignDraftTransactionByIDFlow(private val transactionId: SecureHash) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // REVIEW: should either mark as signed, or drop it
        val wireTransaction = serviceHub.cordaService(DraftTxService::class.java).getDraftTx(transactionId)
            ?: throw IllegalArgumentException("Draft Transaction $transactionId not found");

        return subFlow(SignDraftTransactionFlow(wireTransaction))
    }
}