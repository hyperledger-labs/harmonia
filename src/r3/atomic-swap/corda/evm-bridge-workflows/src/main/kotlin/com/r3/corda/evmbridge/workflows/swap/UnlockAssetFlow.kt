package com.r3.corda.evmbridge.workflows.swap

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.cno.evmbridge.dto.TransactionReceipt
import com.r3.corda.cno.evmbridge.dto.encoded
import com.r3.corda.evmbridge.states.swap.LockState
import com.r3.corda.evmbridge.states.swap.UnlockData
import com.r3.corda.evmbridge.workflows.GetBlockFlow
import com.r3.corda.evmbridge.workflows.GetBlockReceiptsFlow
import com.r3.corda.evmbridge.workflows.GetTransactionFlow
import com.r3.corda.interop.evm.common.trie.PatriciaTrie
import com.r3.corda.interop.evm.common.trie.SimpleKeyValueStore
import net.corda.core.contracts.OwnableState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.loggerFor
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpString
import org.web3j.utils.Numeric

@StartableByRPC
@InitiatingFlow
class UnlockAssetFlow(
    private val transactionId: SecureHash,
    private val blockNumber: Int,
    private val transactionIndex: Int
) : FlowLogic<SignedTransaction>() {

    @Suppress("ClassName")
    companion object {
        object RETRIEVE : ProgressTracker.Step("Retrieving transaction outputs.")
        object QUERY_BLOCK_HEADER : ProgressTracker.Step("Querying block data.")
        object QUERY_BLOCK_RECEIPTS : ProgressTracker.Step("QUERY_BLOCK_RECEIPTS")
        object BUILD_UNLOCK_DATA : ProgressTracker.Step("BUILD_UNLOCK_DATA")
        object UNLOCK_ASSEET : ProgressTracker.Step("UNLOCK_ASSEET")

        fun tracker() = ProgressTracker(
            RETRIEVE,
            QUERY_BLOCK_HEADER,
            QUERY_BLOCK_RECEIPTS,
            BUILD_UNLOCK_DATA,
            UNLOCK_ASSEET
        )

        val log = loggerFor<UnlockAssetFlow>()
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {

        progressTracker.currentStep = RETRIEVE

        val signedTransaction = serviceHub.validatedTransactions.getTransaction(transactionId)
            ?: throw IllegalArgumentException("Transaction not found for ID: $transactionId")

        val outputStateAndRefs = signedTransaction.tx.outputs.mapIndexed { index, state ->
            StateAndRef(state, StateRef(transactionId, index))
        }

        val lockState = outputStateAndRefs
            .filter { it.state.data is LockState }
            .map { serviceHub.toStateAndRef<LockState>(it.ref)}
            .singleOrNull() ?: throw IllegalArgumentException("Transaction $transactionId does not have a lock state")

        val assetState = outputStateAndRefs
            .filter { it.state.data !is LockState }
            .map { serviceHub.toStateAndRef<OwnableState>(it.ref)}
            .singleOrNull() ?: throw IllegalArgumentException("Transaction $transactionId does not have a single asset")

        progressTracker.currentStep = QUERY_BLOCK_HEADER

        // Get the block that mined the transaction that generated the designated EVM event
        val block = subFlow(GetBlockFlow(blockNumber.toBigInteger(), true))

        progressTracker.currentStep = QUERY_BLOCK_RECEIPTS

        // Get all the transaction receipts from the block to build and verify the transaction receipts root
        val receipts = subFlow(GetBlockReceiptsFlow(blockNumber.toBigInteger()))

        // Get the receipt specifically associated with the transaction that generated the event
        val unlockReceipt = receipts[transactionIndex]

        progressTracker.currentStep = BUILD_UNLOCK_DATA

        val validators = lockState.state.data.approvedValidators.map { key ->
            serviceHub.identityService.partyFromKey(key) ?: throw IllegalArgumentException("Key $key does not identify a party")
        }
        // REVIEW: move outside
        val signatures = subFlow(RequestBlockHeaderProofsInitiator(unlockReceipt, validators))

        val merkleProof = generateMerkleProof(receipts, unlockReceipt)

        val unlockData = UnlockData(merkleProof, signatures, block.receiptsRoot, unlockReceipt)

        // REVIEW: notary should be part of the lock state.
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        progressTracker.currentStep = UNLOCK_ASSEET

        return subFlow(UnlockTransactionAndObtainAssetFlow(assetState, lockState, unlockData, notary))
    }

    private fun generateMerkleProof(
        receipts: List<TransactionReceipt>,
        unlockReceipt: TransactionReceipt
    ): SimpleKeyValueStore {
        // Build the trie
        val trie = PatriciaTrie()
        for (receipt in receipts) {
            trie.put(
                encodeKey(receipt.transactionIndex!!),
                receipt.encoded()
            )
        }

        val merkleProof = trie.generateMerkleProof(encodeKey(unlockReceipt.transactionIndex))
        return merkleProof
    }

    private fun encodeKey(key: String?) =
        RlpEncoder.encode(RlpString.create(Numeric.toBigInt(key!!).toLong()))
}