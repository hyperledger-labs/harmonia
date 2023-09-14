package com.interop.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.services.swap.DraftTxService
import com.r3.corda.evminterop.states.swap.LockState
import com.r3.corda.evminterop.workflows.eth2eth.GetBlockFlow
import net.corda.core.crypto.DigitalSignature
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.unwrap
import org.web3j.utils.Numeric
import java.math.BigInteger

typealias CollectBlockSignaturesFlow = SignaturesCollectorFlow.CollectBlockSignaturesFlow

object SignaturesCollectorFlow {

    @CordaSerializable
    data class RequestParams(
        val blockNumber: BigInteger,
        val blocking: Boolean
    )

    @CordaSerializable
    data class RequestParamsWithSignature(
        val blockNumber: BigInteger,
        val blocking: Boolean,
        val signature: DigitalSignature.WithKey
    )

    /**
     * [CollectBlockSignaturesFlow] initiates the signatures collection from the lock-state approved validators
     * asynchronously, blocking or non-blocking. This flow initiates a responder flow on each approved validator so
     * that they can all query the given block as soon as they receive the message and asynchronously report the
     * signature and store it on the initiator node (this) through a secondary flow [CollectorInitiator] that stores
     * the incoming signatures.
     *
     * @param transactionId the transaction hash of the signed draft transaction to unlock
     * @param blockNumber the EVM blockchain block number to query for
     * @param blocking indicates whether the initiating flow will wait for the responder flow to complete
     */
    @Suspendable
    @StartableByRPC
    @InitiatingFlow
    class CollectBlockSignaturesFlow(
        val transactionId: SecureHash,
        val blockNumber: BigInteger,
        val blocking: Boolean
    ) : FlowLogic<Unit>() {

        @Suspendable
        override fun call() {

            val signedTransaction = serviceHub.validatedTransactions.getTransaction(transactionId)
                ?: throw IllegalArgumentException("Transaction not found for ID: $transactionId")

            val lockState = signedTransaction.tx.outputs
                .mapNotNull { it.data as? LockState }
                .singleOrNull()
                ?: throw IllegalArgumentException("Transaction $transactionId does not have a lock state")

            val validators = lockState.approvedValidators.mapNotNull {
                serviceHub.identityService.partyFromKey(it)
            }

            val sessions = validators.map { initiateFlow(it) }

            for(session in sessions) {
                try {
                    session.send(RequestParams(blockNumber, blocking))
                } catch (_: Throwable) {
                    // NOTE: gather as many signatures as possible, ignoring single errors.
                }
            }

            if(blocking) {
                for (session in sessions) {
                    try {
                        session.receive<Boolean>()
                    } catch (_: Throwable) {
                        // NOTE: gather as many signatures as possible, ignoring single errors.
                    }
                }
            }
        }
    }

    @Suspendable
    @StartableByRPC
    @InitiatedBy(CollectBlockSignaturesFlow::class)
    class CollectBlockSignaturesFlowResponder(val session: FlowSession) : FlowLogic<Unit>() {

        @Suspendable
        override fun call() {
            // 2. Validator[i] receives a request to sign
            val request = session.receive<RequestParams>().unwrap { it }

            subFlow(CollectorInitiator(session.counterparty, request.blockNumber, request.blocking))

            if(request.blocking) {
                // send a dummy response to unblock the initiating flow
                session.send(true)
            }
        }
    }

    /**
     * [CollectorInitiator] query the EVM blockchain block and signs it passing the signature to the recipient node.
     *
     * @param recipient the node that will receive the signature.
     * @param blockNumber the EVM blockchain block number to query for.
     * @param blocking indicates whether the initiating flow will wait for the responder flow to complete.
     */
    @Suspendable
    @StartableByRPC
    @InitiatingFlow
    class CollectorInitiator(
        private val recipient: AbstractParty,
        private val blockNumber: BigInteger,
        private val blocking: Boolean
    ) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            // 3. Validator[i] query the EVM, sign the block, and send the signature to the requesting party
            val signature = signReceiptRoot(blockNumber)

            val session = initiateFlow(recipient)

            session.send(RequestParamsWithSignature(blockNumber, blocking, signature))

            if(blocking) {
                // wait for a dummy response before returning to the caller
                session.receive<Boolean>()
            }
        }

        @Suspendable
        fun signReceiptRoot(blockNumber: BigInteger): DigitalSignature.WithKey {
            val block = this.subFlow(GetBlockFlow(blockNumber, true))

            val receiptsRootHash = Numeric.hexStringToByteArray(block.receiptsRoot)

            return this.serviceHub.keyManagementService.sign(receiptsRootHash, ourIdentity.owningKey)
        }
    }

    @Suspendable
    @StartableByRPC
    @InitiatedBy(CollectorInitiator::class)
    class CollectorResponder(private val session: FlowSession) : FlowLogic<Unit?>() {
        @Suspendable
        override fun call() {
            // 4. The requesting party stores the signatures
            val params = session.receive<RequestParamsWithSignature>().unwrap { it }

            serviceHub.cordaService(DraftTxService::class.java).saveBlockSignature(
                params.blockNumber,
                params.signature
            )

            if(params.blocking) {
                // send a dummy response to unblock the initiating flow
                session.send(true)
            }
        }
    }

    /**
     * Helper flow to query the block signatures store for a given block number
     */
    @Suspendable
    @StartableByRPC
    @InitiatingFlow
    class DumpInitiator(
        private val blockNumber: BigInteger
    ) : FlowLogic<String>() {
        @Suspendable
        override fun call(): String {
            val signatures: List<DigitalSignature.WithKey> = serviceHub.cordaService(DraftTxService::class.java).blockSignatures(blockNumber)
            return signatures.map {
                it.by.toString()
            }.joinToString { ", " }
        }
    }
}