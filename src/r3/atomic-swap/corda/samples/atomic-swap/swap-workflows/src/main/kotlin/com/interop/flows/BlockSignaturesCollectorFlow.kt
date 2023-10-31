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
import net.corda.core.utilities.loggerFor
import net.corda.core.utilities.unwrap
import org.web3j.utils.Numeric
import java.math.BigInteger

typealias CollectBlockSignaturesFlow = BlockSignaturesCollectorFlow.CollectBlockSignaturesFlow

object BlockSignaturesCollectorFlow {

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

        companion object {
            val log = loggerFor<CollectBlockSignaturesFlow>()
        }

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

            log.info("Validators: ${validators.first()}")

            val sessions = validators.map { initiateFlow(it) }

            for(session in sessions) {
                try {
                    log.info("Sent request params $blockNumber $blocking")
                    session.send(RequestParams(blockNumber, blocking))
                } catch (e: Throwable) {
                    // NOTE: gather as many signatures as possible, ignoring single errors.
                    log.error("Error while sending response.\nError: $e")
                }
            }

            if(blocking) {
                for (session in sessions) {
                    try {
                        log.info("Receiving response")
                        session.receive<Boolean>()
                        log.info("Received response")
                    } catch (e: Throwable) {
                        // NOTE: gather as many signatures as possible, ignoring single errors.
                        log.error("Error while receiving response.\nError: $e")
                    }
                }
            }
        }
    }

    @Suspendable
    @StartableByRPC
    @InitiatedBy(CollectBlockSignaturesFlow::class)
    class CollectBlockSignaturesFlowResponder(val session: FlowSession) : FlowLogic<Unit>() {

        companion object {
            val log = loggerFor<CollectBlockSignaturesFlowResponder>()
        }

        @Suspendable
        override fun call() {
            // 2. Validator[i] receives a request to sign
            val request = session.receive<RequestParams>().unwrap { it }
            log.info("Received request params $request.blockNumber $request.blocking")

            log.info("Initiating collector subflow")
            subFlow(CollectorInitiator(session.counterparty, request.blockNumber, request.blocking))
            log.info("Initiated collector subflow")

            if(request.blocking) {
                // send a dummy response to unblock the initiating flow
                log.info("Sending dummy response")
                try {
                    session.send(true)
                } catch (e: Throwable) {
                    log.error("Error while sending response.\nError: $e")
                }
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

        companion object {
            val log = loggerFor<CollectorInitiator>()
        }

        @Suspendable
        override fun call() {
            // 3. Validator[i] query the EVM, sign the block, and send the signature to the requesting party
            log.info("Signing receipt root")
            val signature = signReceiptRoot(blockNumber)
            log.info("Signed receipt root")

            val session = initiateFlow(recipient)

            log.info("Sending request params with signature")
            session.send(RequestParamsWithSignature(blockNumber, blocking, signature))
            log.info("Sent request params with signature")

            if(blocking) {
                // wait for a dummy response before returning to the caller
                log.info("Waiting dummy response")
                try {
                    session.receive<Boolean>()
                } catch (e: Throwable) {
                    log.error("Error while receiving response.\nError: $e")
                }
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

        companion object {
            val log = loggerFor<CollectorResponder>()
        }

        @Suspendable
        override fun call() {
            // 4. The requesting party stores the signatures
            val params = try {
                session.receive<RequestParamsWithSignature>().unwrap { it }
            } catch (e: Throwable) {
                log.error("Error while receiving response.\nError: $e")
                throw e
            }

            log.info("Received request params with signature")

            log.info("Saving block signature")
            serviceHub.cordaService(DraftTxService::class.java).saveBlockSignature(
                params.blockNumber,
                params.signature
            )
            log.info("Saved block signature")

            if(params.blocking) {
                // send a dummy response to unblock the initiating flow
                log.info("Sending dummy response")
                session.send(true)
                log.info("Sending dummy response")
            }
        }
    }

    /**
     * Helper flow to query the block signatures store for a given block number
     */
    @Suspendable
    @StartableByRPC
    @InitiatingFlow
    class S(
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
