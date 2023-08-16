package com.r3.corda.evmbridge.workflows.swap

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.cno.evmbridge.dto.TransactionReceipt
import com.r3.corda.cno.evmbridge.dto.TransactionReceiptLog
import com.r3.corda.evmbridge.states.swap.SwapTransactionDetails
import com.r3.corda.evmbridge.workflows.GetBlockFlow
import com.r3.corda.evmbridge.workflows.GetTransactionReceiptFlow
import net.corda.core.crypto.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.unwrap
import org.web3j.utils.Numeric

/**
 * Initiating flow which requests validation and attestation of an EVM event from a pool of approved validators.
 */
@StartableByRPC
@InitiatingFlow
class RequestBlockHeaderProofs(
    val swapTxId: SecureHash,
    val swapTxDetails: SwapTransactionDetails,
    val transactionReceipt: TransactionReceipt,
    val validators: List<Party>
) : FlowLogic<List<DigitalSignature.WithKey>>() {

    @CordaSerializable
    internal data class RequestData(
        val swapTxId: SecureHash,
        val swapTxDetails: SwapTransactionDetails,
        val transactionReceipt: TransactionReceipt
    )

    @Suspendable
    override fun call(): List<DigitalSignature.WithKey> {
        return validators.map { validator ->
            val session = initiateFlow(validator)
            session.send(RequestData(swapTxId, swapTxDetails, transactionReceipt))
            session.receive<DigitalSignature.WithKey>().unwrap { it }
        }
    }
}

/**
 * Responder flow which validates and attests (signs) an EVM event
 */
@InitiatedBy(RequestBlockHeaderProofs::class)
class RequestBlockHeaderProofsResponder(val session: FlowSession) : FlowLogic<Unit?>() {

    @Suspendable
    override fun call() {
        // Receive swap transaction details required to gather EVM proof and validate it
        val requestData = session.receive<RequestBlockHeaderProofs.RequestData>().unwrap { it }

        // get the block that requires signature over the receipts root
        val block = subFlow(GetBlockFlow(requestData.transactionReceipt.blockNumber, true))

        val receiptsRootHash = Numeric.hexStringToByteArray(block.receiptsRoot)
        // TODO: implement proof gathering; for now return signature over the swap transaction ID
        val sig = serviceHub.keyManagementService.sign(receiptsRootHash, ourIdentity.owningKey)
        session.send(sig)
    }
}