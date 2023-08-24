package com.r3.corda.evmbridge.workflows.swap

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evmbridge.services.swap.DraftTxService
import com.r3.corda.evmbridge.states.swap.LockState
import net.corda.core.contracts.requireThat
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignableData
import net.corda.core.crypto.SignatureMetadata
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction

/*
val signedTx = await(bob.startFlow(SignDraftTransactionFlow(draftTx)))

        // Alice initiates a forward transfer of the Corda asset unlocking it using the proofs she collects from the
        // parties designed to validate/sign the block header for the expected block number
        val lockedAsset = alice.services.vaultService.queryBy(GenericAssetState::class.java).states.single()

        val lockState = alice.services.vaultService.queryBy(LockState::class.java).states.single()

        // Generate proof required for Alice to unlock the lock-state and take ownership of the Corda asset
        val validatorSignatures = await(alice.startFlow(
            RequestBlockHeaderProofs(lockedAsset.ref.txhash, swapDetails, txReceipt, listOf(charlie.toParty()))
        ))

        // Get the block that mined the transaction that generated the designated EVM event
        val block = alice.startFlow(GetBlockFlow(txReceipt.blockNumber, true)).getOrThrow()

        // Get all the transaction receipts from the block to build and verify the transaction receipts root
        val receipts = alice.startFlow(GetBlockReceiptsFlow(txReceipt.blockNumber)).getOrThrow()

        // Build the trie
        val trie = PatriciaTrie()
        for(receipt in receipts) {
            trie.put(
                RlpEncoder.encode(RlpString.create(Numeric.toBigInt(receipt.transactionIndex!!).toLong())),
                receipt.encoded()
            )
        }

        // verify the trie against the block's stored receipts root
        val verified = PatriciaTrie.verifyMerkleProof(
            Numeric.hexStringToByteArray(block.receiptsRoot),
            RlpEncoder.encode(RlpString.create(Numeric.toBigInt(txReceipt.transactionIndex!!).toLong())),
            txReceipt.encoded(),
            merkleProof
        )
        assert(verified) { "Failed to verify the receipts root" }

        // Gather the data for the unlock command and create the unlock transaction that moves the asset from Alice
        // to the expected recipient Bob in response to the `forwardTransferEvent` event
        val unlockData = UnlockData(merkleProof, validatorSignatures, block.receiptsRoot, txReceipt)
        assertFailsWith<TransactionVerificationException.ContractRejection> {
            await(alice.startFlow(UnlockTransactionAndObtainAssetFlow(lockedAsset, lockState, unlockData, notary.toParty())))
        }
* */



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
        // TODO: Initiate EVM asset transfer as a result of draft transaction being finalized
    }
}