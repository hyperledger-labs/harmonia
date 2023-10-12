package com.r3.corda.evminterop.workflows.swap

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.services.swap.DraftTxService
import com.r3.corda.evminterop.states.swap.LockState
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction

/**
 * Initiating flow which takes a draft transaction and attempts to sign and notarize it.
 */
@StartableByRPC
@InitiatingFlow
class SignDraftTransactionFlow(private val draftTx: WireTransaction) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // Sign the draft transaction
        val signatureMetadata = SignatureMetadata(
            serviceHub.myInfo.platformVersion,
            Crypto.findSignatureScheme(ourIdentity.owningKey).schemeNumberID
        )
        val signableData = SignableData(draftTx.id, signatureMetadata)
        val sig = serviceHub.keyManagementService.sign(signableData, ourIdentity.owningKey)

        val sessions = (draftTx.outputsOfType<LockState>().single().participants - ourIdentity).map { initiateFlow(it) }
        val stx = SignedTransaction(draftTx, listOf(sig))
        return subFlow(FinalityFlow(stx, sessions))
    }
}

/**
 * Responder flow which receives a finalized transaction
 */
@InitiatedBy(SignDraftTransactionFlow::class)
class SignDraftTransactionFlowResponder(val session: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val txService = serviceHub.cordaService(DraftTxService::class.java)
        val receiveSignedTransactionFlow = object : ReceiveTransactionFlow(session, statesToRecord = StatesToRecord.ALL_VISIBLE) {
            override fun checkBeforeRecording(stx: SignedTransaction) = requireThat {
                // Signing only a transaction we agreed to previously
                val draftTx = txService.getDraftTx(stx.id)
                "No unsigned draft transaction found " using (draftTx != null)
                "Transaction to be signed has a different TX ID from the draft transaction" using (draftTx!!.id == stx.id)
            }
        }

        val stx = subFlow(receiveSignedTransactionFlow)
        txService.deleteDraftTx(stx.id)
        // NOTE: Initiate EVM asset transfer as a result of draft transaction being finalized
    }
}
