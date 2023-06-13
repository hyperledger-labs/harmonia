package com.r3.corda.evmbridge.workflows.swap

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evmbridge.states.swap.SwapTransactionDetails
import net.corda.core.crypto.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.utilities.unwrap

/**
 * Initiating flow which requests validation and attestation of an EVM event from a pool of approved validators.
 */
@StartableByRPC
@InitiatingFlow
class RequestBlockHeaderProofs(val swapTxId: SecureHash, val swapTxDetails: SwapTransactionDetails, val validators: List<Party>) : FlowLogic<List<DigitalSignature.WithKey>>() {

    @Suspendable
    override fun call(): List<DigitalSignature.WithKey> {
        return validators.map { validator ->
            val session = initiateFlow(validator)
            session.send(swapTxId)
            session.send(swapTxDetails)
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
        val swapTxId = session.receive<SecureHash>().unwrap { it }
        val swapTxDetails = session.receive<SwapTransactionDetails>().unwrap { it }

        // TODO: implement proof gathering; for now return signature over the swap transaction ID
        // Sign the draft transaction ID
        val sig = serviceHub.keyManagementService.sign(swapTxId.bytes, ourIdentity.owningKey)
        session.send(sig)
    }
}