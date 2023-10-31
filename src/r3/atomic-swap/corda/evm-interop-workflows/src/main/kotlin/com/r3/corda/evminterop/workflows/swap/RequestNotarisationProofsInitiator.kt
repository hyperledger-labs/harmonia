package com.r3.corda.evminterop.workflows.swap

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.services.evmInterop
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.utilities.unwrap

typealias RequestNotarizationProofsInitiator = RequestNotarizationProofs.Initiator

object RequestNotarizationProofs {

    /**
     * Initiating flow which requests validation and attestation of an EVM event from a pool of approved validators.
     */
    @StartableByRPC
    @InitiatingFlow
    class Initiator(
        val transactionId: SecureHash,
        private val validators: List<Party>
    ) : FlowLogic<List<ByteArray>>() {

        @Suspendable
        override fun call(): List<ByteArray> {
            return validators.map { validator ->
                if(validator.owningKey == ourIdentity.owningKey) {
                    serviceHub.evmInterop().web3Provider().signData(transactionId.bytes)
                } else {
                    val session = initiateFlow(validator)
                    session.send(transactionId)
                    session.receive<ByteArray>().unwrap { it }
                }
            }
        }
    }

    /**
     * Responder flow which validates and attests (signs) an EVM event
     */
    @InitiatedBy(Initiator::class)
    class Responder(val session: FlowSession) : FlowLogic<Unit?>() {

        @Suspendable
        override fun call() {
            val transactionId = session.receive<SecureHash>().unwrap { it }

            val signature = serviceHub.evmInterop().web3Provider().signData(transactionId.bytes)

            session.send(signature)
        }
    }
}
