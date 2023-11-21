package com.r3.corda.evminterop.workflows.internal

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.dto.TransactionReceipt
import com.r3.corda.evminterop.dto.TransactionReceiptLog
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.unwrap
import java.security.PublicKey

/**
 * Helper function to replicate a TransactionReceipt to a Corda serializable version of it.
 */
fun org.web3j.protocol.core.methods.response.TransactionReceipt.toSerializable(): TransactionReceipt {
    return TransactionReceipt(
        transactionHash = transactionHash,
        transactionIndex = transactionIndexRaw,
        blockHash = blockHash,
        blockNumber = blockNumber,
        cumulativeGasUsed = cumulativeGasUsedRaw,
        gasUsed = gasUsedRaw,
        contractAddress = contractAddress,
        root = root,
        status = status,
        from = from,
        to = to,
        logs = this.logs.map {
            TransactionReceiptLog(
                it.isRemoved,
                it.logIndexRaw,
                it.transactionIndexRaw,
                it.transactionHash,
                it.blockHash,
                it.blockNumber,
                it.address,
                it.data,
                it.type,
                it.topics
            )
        },
        logsBloom = logsBloom
    )
}

/**
 * Create a composite key from two identities, and register it as the local node's identity.
 * @param ourParty the registrant's identity.
 * @param otherParty the other identity to compose the owning key with.
 * @return the [CompositeKey] from the two identities.
 */
@Suspendable
fun ServiceHub.registerCompositeKey(ourParty: Party, otherParty: Party): PublicKey {
    val compositeKey = CompositeKey.Builder()
        .addKey(ourParty.owningKey, weight = 1)
        .addKey(otherParty.owningKey, weight = 1)
        .build(1)

    identityService.registerKey(compositeKey, ourParty)

    return compositeKey
}

@Suspendable
fun ServiceHub.registerCompositeKey(ourParty: Party, compositeKey: PublicKey): PublicKey {
    identityService.registerKey(compositeKey, ourParty)
    return compositeKey
}

/**
 * Collect signatures for the provided [SignedTransaction], from the list of [Party] provided.
 * This is an initiating flow, and is used where some required signatures are from [CompositeKey]s.
 * The standard Corda CollectSignaturesFlow will not work in this case.
 * @param stx - the [SignedTransaction] to sign
 * @param signers - the list of signing [Party]s
 */
@InitiatingFlow
internal class CollectSignaturesForComposites(
    private val stx: SignedTransaction,
    private val signers: List<Party>
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // create new sessions to signers and trigger the signing responder flow
        val sessions = signers.map { initiateFlow(it) }

        // We filter out any responses that are not TransactionSignature`s (i.e. refusals to sign).
        val signatures = sessions
            .map { it.sendAndReceive<Any>(stx).unwrap { data -> data } }
            .filterIsInstance<TransactionSignature>()
        return stx.withAdditionalSignatures(signatures)
    }
}

/**
 * Responder flow for [CollectSignaturesForComposites] flow.
 */
@InitiatedBy(CollectSignaturesForComposites::class)
internal class CollectSignaturesForCompositesHandler(private val otherPartySession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {

        otherPartySession.receive<SignedTransaction>().unwrap { partStx ->
            // REVIEW: add conditions where we might not sign?

            val returnStatus = serviceHub.createSignature(partStx)
            otherPartySession.send(returnStatus)
        }
    }
}
