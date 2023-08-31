package com.r3.corda.evminterop.workflows.swap

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.dto.TransactionReceipt
import com.r3.corda.evminterop.workflows.GetBlockFlow
import net.corda.core.crypto.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.utilities.unwrap
import org.web3j.utils.Numeric

typealias RequestBlockHeaderProofsInitiator = RequestBlockHeaderProofs.Initiator

object RequestBlockHeaderProofs {

    /**
     * Initiating flow which requests validation and attestation of an EVM event from a pool of approved validators.
     */
    @StartableByRPC
    @InitiatingFlow
    class Initiator(
        val transactionReceipt: TransactionReceipt,
        val validators: List<Party>
    ) : FlowLogic<List<DigitalSignature.WithKey>>() {

        @Suspendable
        override fun call(): List<DigitalSignature.WithKey> {
            return validators.map { validator ->
                if(validator.owningKey == ourIdentity.owningKey) {
                    signReceiptRoot(transactionReceipt)
                } else {
                    val session = initiateFlow(validator)
                    session.send(transactionReceipt)
                    session.receive<DigitalSignature.WithKey>().unwrap { it }
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
            // Receive swap transaction details required to gather EVM proof and validate it
            val transactionReceipt = session.receive<TransactionReceipt>().unwrap { it }

            val sig = this.signReceiptRoot(transactionReceipt)

            session.send(sig)
        }
    }

    @Suspendable
    fun FlowLogic<*>.signReceiptRoot(transactionReceipt: TransactionReceipt): DigitalSignature.WithKey {
        // get the block that requires signature over the receipts root
        val block = this.subFlow(GetBlockFlow(transactionReceipt.blockNumber, true))

        val receiptsRootHash = Numeric.hexStringToByteArray(block.receiptsRoot)

        return this.serviceHub.keyManagementService.sign(receiptsRootHash, ourIdentity.owningKey)
    }
}