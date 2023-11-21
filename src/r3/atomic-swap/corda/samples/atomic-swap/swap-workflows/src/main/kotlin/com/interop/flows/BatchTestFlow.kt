package com.interop.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.evminterop.*
import com.r3.corda.evminterop.dto.Block
import com.r3.corda.evminterop.dto.TransactionReceipt
import com.r3.corda.evminterop.dto.encoded
import com.r3.corda.evminterop.services.swap.DraftTxService
import com.r3.corda.evminterop.workflows.GenericAssetSchemaV1
import com.r3.corda.evminterop.workflows.GenericAssetState
import com.r3.corda.evminterop.workflows.IssueGenericAssetFlow
import com.r3.corda.evminterop.workflows.eth2eth.GetBlockFlow
import com.r3.corda.evminterop.workflows.eth2eth.GetBlockReceiptsFlow
import com.r3.corda.evminterop.workflows.swap.*
import com.r3.corda.interop.evm.common.trie.PatriciaTrie
import com.r3.corda.interop.evm.common.trie.SimpleKeyValueStore
import net.corda.core.contracts.OwnableState
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.unwrap
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpString
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*

@StartableByRPC
@InitiatingFlow
class BatchTestFlow(private val count: Int) : FlowLogic<Unit>() {

    constructor() : this(1)

    val alice = "O=Alice, L=London, C=GB"
    val bob = "O=Bob, L=San Francisco, C=US"
    val charlie = "O=Charlie, L=Mumbai, C=IN"

    lateinit var aliceParty: Party
    lateinit var bobParty: Party
    lateinit var charlieParty: Party

    @Suspendable
    override fun call(): Unit {

        val identityService = serviceHub.identityService

        aliceParty = identityService.wellKnownPartyFromX500Name(CordaX500Name.parse(alice))!!
        bobParty = identityService.wellKnownPartyFromX500Name(CordaX500Name.parse(bob))!!
        charlieParty = identityService.wellKnownPartyFromX500Name(CordaX500Name.parse(charlie))!!

        for (i in 0..count) {
            `bob can unlock corda asset by asynchronous collection of block signatures`()
            `alice can unlock corda asset by asynchronous collection of block signatures`()
            `bob can transfer evm asset by asynchronous collection of notarisation signatures`()
        }
    }

    protected var aliceAddress = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8"
    protected var bobAddress = "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC"
    protected var charlieAddress = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266"

    private val alicePrivateKey = "0x59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690d"
    private val bobPrivateKey = "0x5de4111afa1a4b94908f83103eb1f1706367c2e68ca870fc3fb9a804cdab365a"
    private val charliePrivateKey = "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80"

    protected val evmDeployerAddress = "0x5067457698Fd6Fa1C6964e416b3f42713513B3dD"
    protected val protocolAddress = "0x70e0bA845a1A0F2DA3359C97E0285013525FFC49"
    protected val goldTokenDeployAddress = "0x5FbDB2315678afecb367f032d93F642f64180aa3"
    protected val silverTokenDeployAddress = "0xc6e7DF5E7b4f2A278906862b61205850344D4e7d"


    private val amount = 1.toBigInteger()

    @Suspendable
    fun `bob can unlock corda asset by asynchronous collection of block signatures`() {
        val assetName = UUID.randomUUID().toString()

        // Create Corda asset owned by Bob
        val assetTx : StateRef = subFlow(RequestIssueGenericAssetFlow(assetName, bobParty))

        // Prepare the generic `claim / revert` event expectation.
        // Note that this is not the encoded event but the event encoder. It does not include the draft transaction hash,
        // which is only known after the draft transaction is created. Therefore, the encoder builds the encoded event
        // only when a new transaction consumes the draft transaction outputs, using their state-ref to build the full
        // encoded event.
        val swapVaultEventEncoder = SwapVaultEventEncoder.create(
            chainId = BigInteger.valueOf(1337),
            protocolAddress = protocolAddress,
            owner = aliceAddress,
            recipient = bobAddress,
            amount = amount,
            tokenId = BigInteger.ZERO,
            tokenAddress = goldTokenDeployAddress,
            signaturesThreshold = BigInteger.ONE,
            signers = listOf(charlieAddress) // same as validators but the EVM identity instead
        )

        // Draft the Corda Asset transfer that can be transferred to the recipient or reverted to the owner if valid
        // EVM event proofs are presented for the claim / revert transaction events from the expected protocol address
        // and draft transaction hash (swap id).
        val draftTxHash = subFlow(RequestDraftAssetSwapFlow(
            assetTx.txhash,
            assetTx.index,
            aliceParty,
            serviceHub.networkMapCache.notaryIdentities.first(),
            listOf(charlieParty as AbstractParty),
            1,
            swapVaultEventEncoder,
            bobParty
        ))

        // Sign the draft transaction. In real use cases, this only happens after the counterparty (i.e.: alice) signals
        // the acceptance of the draft transaction and the willing to continue with the swap with a commit of the
        // counterparty EVM asset.
        val stx = subFlow(RequestSignDraftTransactionByIDFlow(draftTxHash, bobParty))

        // counterparty (alice, EVM) commits the asset and claims it in favour of the recipient (bob, EVM address)
        val (txReceipt, leafKey, merkleProof) = commitAndClaim(
            draftTxHash, amount, aliceParty, bobAddress, BigInteger.ONE, listOf(charlieAddress)
        )

        // bob collects signatures form oracles/validators of the block containing the claim's transfer event
        // asynchronously for the given transaction id
        subFlow(RequestCollectBlockSignaturesFlow(draftTxHash, txReceipt.blockNumber, true, bobParty))

        // Unlock and finalize the transfer to the recipient by producing and presenting proofs (that the EVM asset was
        // transferred to the expected recipient) to the lock contract verified during the new transaction.
        val utx = subFlow(
            RequestUnlockAssetFlow(
                stx.tx.id,
                txReceipt.blockNumber,
                Numeric.toBigInt(txReceipt.transactionIndex!!),
                bobParty
            )
        )

        // Verify the unlocked asset is now owned by Alice and not anymore from Bob
        require(
            aliceParty.owningKey ==
            (utx.tx.outputStates.single() as OwnableState).owner.owningKey
        )
        // Verify that bob can't see the locked asset anymore
        require(serviceHub.vaultService.queryBy(GenericAssetState::class.java, queryCriteria(assetName)).states.isEmpty())
    }

    @Suspendable
    fun `alice can unlock corda asset by asynchronous collection of block signatures`() {
        val assetName = UUID.randomUUID().toString()

        // Create Corda asset owned by Bob
        val assetTx : StateRef = subFlow(RequestIssueGenericAssetFlow(assetName, bobParty))

        // Prepare the generic `claim / revert` event expectation.
        // Note that this is not the encoded event but the event encoder. It does not include the draft transaction hash,
        // which is only known after the draft transaction is created. Therefore, the encoder builds the encoded event
        // only when a new transaction consumes the draft transaction outputs, using their state-ref to build the full
        // encoded event.
        val swapVaultEventEncoder = SwapVaultEventEncoder.create(
            chainId = BigInteger.valueOf(1337),
            protocolAddress = protocolAddress,
            owner = aliceAddress,
            recipient = bobAddress,
            amount = amount,
            tokenId = BigInteger.ZERO,
            tokenAddress = goldTokenDeployAddress,
            signaturesThreshold = BigInteger.ONE,
            signers = listOf(charlieAddress) // same as validators but the EVM identity instead
        )

        // Draft the Corda Asset transfer that can be transferred to the recipient or reverted to the owner if valid
        // EVM event proofs are presented for the claim / revert transaction events from the expected protocol address
        // and draft transaction hash (swap id).
        val draftTxHash = subFlow(RequestDraftAssetSwapFlow(
            assetTx.txhash,
            assetTx.index,
            aliceParty,
            serviceHub.networkMapCache.notaryIdentities.first(),
            listOf(charlieParty as AbstractParty),
            1,
            swapVaultEventEncoder,
            bobParty
        ))

        // Sign the draft transaction. In real use cases, this only happens after the counterparty (i.e.: alice) signals
        // the acceptance of the draft transaction and the willing to continue with the swap with a commit of the
        // counterparty EVM asset.
        val stx = subFlow(RequestSignDraftTransactionByIDFlow(draftTxHash, bobParty))

        // counterparty (alice, EVM) commits the asset and claims it in favour of the recipient (bob, EVM address)
        val (txReceipt, leafKey, merkleProof) = commitAndClaim(
            draftTxHash, amount, aliceParty, bobAddress, BigInteger.ONE, listOf(charlieAddress)
        )

        // alice collects signatures form oracles/validators of the block containing the claim's transfer event
        // asynchronously for the given transaction id
        subFlow(RequestCollectBlockSignaturesFlow(draftTxHash, txReceipt.blockNumber, true, aliceParty))

        // Unlock and finalize the transfer to the recipient by producing and presenting proofs (that the EVM asset was
        // transferred to the expected recipient) to the lock contract verified during the new transaction.
        val utx = subFlow(
            RequestUnlockAssetFlow(
                stx.tx.id,
                txReceipt.blockNumber,
                Numeric.toBigInt(txReceipt.transactionIndex!!),
                aliceParty
            )
        )

        // Verify the unlocked asset is now owned by Alice and not anymore from Bob
        require(
            aliceParty.owningKey ==
            (utx.tx.outputStates.single() as OwnableState).owner.owningKey
        )
        // Verify that bob can't see the locked asset anymore
        require(serviceHub.vaultService.queryBy(GenericAssetState::class.java, queryCriteria(assetName)).states.isEmpty())
    }

    @Suspendable
    fun `bob can transfer evm asset by asynchronous collection of notarisation signatures`() {
        val assetName = UUID.randomUUID().toString()

        // Create Corda asset owned by Bob
        val assetTx : StateRef = subFlow(RequestIssueGenericAssetFlow(assetName, bobParty))

        // Prepare the generic `claim / revert` event expectation.
        // Note that this is not the encoded event but the event encoder. It does not include the draft transaction hash,
        // which is only known after the draft transaction is created. Therefore, the encoder builds the encoded event
        // only when a new transaction consumes the draft transaction outputs, using their state-ref to build the full
        // encoded event.
        val swapVaultEventEncoder = SwapVaultEventEncoder.create(
            chainId = BigInteger.valueOf(1337),
            protocolAddress = protocolAddress,
            owner = aliceAddress,
            recipient = bobAddress,
            amount = amount,
            tokenId = BigInteger.ZERO,
            tokenAddress = goldTokenDeployAddress,
            signaturesThreshold = BigInteger.ONE,
            signers = listOf(charlieAddress) // same as validators but the EVM identity instead
        )

        // Draft the Corda Asset transfer that can be transferred to the recipient or reverted to the owner if valid
        // EVM event proofs are presented for the claim / revert transaction events from the expected protocol address
        // and draft transaction hash (swap id).
        val draftTxHash = subFlow(RequestDraftAssetSwapFlow(
            assetTx.txhash,
            assetTx.index,
            aliceParty,
            serviceHub.networkMapCache.notaryIdentities.first(),
            listOf(charlieParty as AbstractParty),
            1,
            swapVaultEventEncoder,
            bobParty
        ))

        // Alice commits her asset to the protocol contract
        val commitTxReceipt: TransactionReceipt = subFlow(
            RequestCommitWithTokenFlow(draftTxHash, goldTokenDeployAddress, amount, bobAddress, BigInteger.ONE, listOf(charlieAddress), aliceParty)
        )

        // Sign the draft transaction.
        val stx = subFlow(RequestSignDraftTransactionByIDFlow(draftTxHash, bobParty))

        // bob collects evm signatures from charlie
        subFlow(RequestCollectNotarizationSignaturesFlow(draftTxHash, true, bobParty))

        // collect the EVM verifiable signatures that attest that the draft transaction was signed by the notary
        val signatures = serviceHub.cordaService(DraftTxService::class.java).notarizationProofs(draftTxHash)

        // Bob can claim Alice's EVM committed asset
        val txReceipt: TransactionReceipt = subFlow(
            RequestClaimCommitmentWithSignaturesFlow(draftTxHash, signatures, bobParty)
        )

        // alice collects signatures form oracles/validators of the block containing the claim's transfer event
        // asynchronously for the given transaction id
        subFlow(RequestCollectBlockSignaturesFlow(draftTxHash, txReceipt.blockNumber, true, aliceParty))

        // Unlock and finalize the transfer to the recipient by producing and presenting proofs (that the EVM asset was
        // transferred to the expected recipient) to the lock contract verified during the new transaction.
        val utx = subFlow(
            RequestUnlockAssetFlow(
                stx.tx.id,
                txReceipt.blockNumber,
                Numeric.toBigInt(txReceipt.transactionIndex!!),
                aliceParty
            )
        )

        // Verify the unlocked asset is now owned by Alice and not anymore from Bob
        require(
            aliceParty.owningKey ==
            (utx.tx.outputStates.single() as OwnableState).owner.owningKey
        )
        // Verify that bob can't see the locked asset anymore
        require(serviceHub.vaultService.queryBy(GenericAssetState::class.java, queryCriteria(assetName)).states.isEmpty())
    }

    @Suspendable
    private fun queryCriteria(assetName: String): QueryCriteria.VaultCustomQueryCriteria<GenericAssetSchemaV1.PersistentGenericAsset> {
        return builder {
            QueryCriteria.VaultCustomQueryCriteria(
                GenericAssetSchemaV1.PersistentGenericAsset::assetName.equal(
                    assetName
                )
            )
        }
    }

    @Suspendable
    protected fun commitAndClaim(
        transactionId: SecureHash,
        amount: BigInteger,
        senderNode: Party,
        recipientAddress: String,
        threshold: BigInteger,
        signers: List<String>
    ) : Triple<TransactionReceipt, ByteArray, SimpleKeyValueStore> {

        val commitTxReceipt: TransactionReceipt = subFlow(
            RequestCommitWithTokenFlow(transactionId, goldTokenDeployAddress, amount, recipientAddress, threshold, signers, senderNode)
        )

        val claimTxReceipt: TransactionReceipt = subFlow(
            RequestClaimCommitmentFlow(transactionId, senderNode)
        )

        // get the block that mined the ERC20 `Transfer` Transaction
        val block = subFlow(
            RequestGetBlockFlow(claimTxReceipt.blockNumber, true, senderNode)
        )

        // get all transaction receipts from the block that mined the ERC20 `Transfer` Transaction
        val receipts = subFlow(
            RequestGetBlockReceiptsFlow(claimTxReceipt.blockNumber, senderNode)
        )

        // Build the Patricia Trie from the Block receipts and verify it's valid
        val trie = PatriciaTrie()
        for(receipt in receipts) {
            trie.put(
                RlpEncoder.encode(RlpString.create(Numeric.toBigInt(receipt.transactionIndex!!).toLong())),
                receipt.encoded()
            )
        }

        // generate a proof for the transaction receipt that belong to the ERC20 transfer transaction
        val claimKey = RlpEncoder.encode(RlpString.create(Numeric.toBigInt(claimTxReceipt.transactionIndex!!).toLong()))
        val transactionProof = trie.generateMerkleProof(claimKey) as SimpleKeyValueStore

        return Triple(claimTxReceipt, claimKey, transactionProof)
    }

}

// ---------------------------------------------------------------------------------------------------------------------

@InitiatingFlow
@StartableByRPC
class RequestGetBlockFlow(
    private val hash: String,
    private val includeTransactions: Boolean,
    private val targetParty: Party
) : FlowLogic<Block>() {
    private lateinit var number: BigInteger

    constructor(number: BigInteger, includeTransactions: Boolean, targetParty: Party) : this("", includeTransactions, targetParty) {
        this.number = number
    }

    @Suspendable
    override fun call(): Block {
        return if (ourIdentity == targetParty) {
            subFlow(GetBlockFlow(hash, includeTransactions))
        } else {
            val session = initiateFlow(targetParty)
            if (hash.isEmpty()) {
                session.send(number)
            } else {
                session.send(hash)
            }
            session.send(includeTransactions)
            return session.receive<Block>().unwrap { it }
        }
    }
}

@InitiatedBy(RequestGetBlockFlow::class)
class RespondToGetBlockFlow(private val session: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val hashOrNumber = session.receive<Any>().unwrap { it }
        val includeTransactions = session.receive<Boolean>().unwrap { it }
        val block = if (hashOrNumber is String) {
            subFlow(GetBlockFlow(hashOrNumber, includeTransactions))
        } else if (hashOrNumber is BigInteger) {
            subFlow(GetBlockFlow(hashOrNumber, includeTransactions))
        } else {
            throw IllegalArgumentException("Invalid hash or number type")
        }
        session.send(block)
    }
}

// ---------------------------------------------------------------------------------------------------------------------

@InitiatingFlow
@StartableByRPC
class RequestIssueGenericAssetFlow(private val assetName: String, private val party: Party) : FlowLogic<StateRef>() {
    @Suspendable
    override fun call(): StateRef {
        return if (ourIdentity == party) {
            subFlow(IssueGenericAssetFlow(assetName))
        } else {
            val session = initiateFlow(party)
            session.send(assetName)
            return session.receive<StateRef>().unwrap { it }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------

@InitiatedBy(RequestIssueGenericAssetFlow::class)
class SendAndReceiveStateRefFlow(private val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val assetName = counterpartySession.receive<String>().unwrap { it }
        val assetTx = subFlow(IssueGenericAssetFlow(assetName))
        counterpartySession.send(assetTx)
    }
}

// ---------------------------------------------------------------------------------------------------------------------

@InitiatingFlow
@StartableByRPC
class RequestDraftAssetSwapFlow(
    private val transactionId: SecureHash,
    private val outputIndex: Int,
    private val recipient: AbstractParty,
    private val notary: AbstractParty,
    private val validators: List<AbstractParty>,
    private val signaturesThreshold: Int,
    private val unlockEvent: SwapVaultEventEncoder,
    private val targetParty: Party
) : FlowLogic<SecureHash>() {
    @Suspendable
    override fun call(): SecureHash {
        return if (ourIdentity == targetParty) {
            // Directly call DraftAssetSwapFlow if on targetParty's node
            subFlow(DraftAssetSwapFlow(
                transactionId, outputIndex, recipient, notary,
                validators, signaturesThreshold, unlockEvent
            ))
        } else {
            val session = initiateFlow(targetParty)
            session.send(transactionId)
            session.send(outputIndex)
            session.send(recipient)
            session.send(notary)
            session.send(validators)
            session.send(signaturesThreshold)
            session.send(unlockEvent)
            return session.receive<SecureHash>().unwrap { it }
        }
    }
}

@InitiatedBy(RequestDraftAssetSwapFlow::class)
class RespondToDraftAssetSwapFlow(private val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val transactionId = counterpartySession.receive<SecureHash>().unwrap { it }
        val outputIndex = counterpartySession.receive<Int>().unwrap { it }
        val recipient = counterpartySession.receive<AbstractParty>().unwrap { it }
        val notary = counterpartySession.receive<AbstractParty>().unwrap { it }
        val validators = counterpartySession.receive<List<AbstractParty>>().unwrap { it }
        val signaturesThreshold = counterpartySession.receive<Int>().unwrap { it }
        val unlockEvent = counterpartySession.receive<SwapVaultEventEncoder>().unwrap { it }

        val txId = subFlow(DraftAssetSwapFlow(
            transactionId, outputIndex, recipient, notary,
            validators, signaturesThreshold, unlockEvent
        ))
        counterpartySession.send(txId)
    }
}

// ---------------------------------------------------------------------------------------------------------------------

@InitiatingFlow
@StartableByRPC
class RequestSignDraftTransactionByIDFlow(
    private val transactionId: SecureHash,
    private val targetParty: Party
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return if (ourIdentity == targetParty) {
            // Directly call SignDraftTransactionByIDFlow if on targetParty's node
            subFlow(SignDraftTransactionByIDFlow(transactionId))
        } else {
            // Start a session with targetParty and send transactionId
            val session = initiateFlow(targetParty)
            session.send(transactionId)
            // Receive the SignedTransaction response from targetParty
            return session.receive<SignedTransaction>().unwrap { it }
        }
    }
}

@InitiatedBy(RequestSignDraftTransactionByIDFlow::class)
class RespondToSignDraftTransactionByIDFlow(private val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val transactionId = counterpartySession.receive<SecureHash>().unwrap { it }
        val signedTx = subFlow(SignDraftTransactionByIDFlow(transactionId))
        counterpartySession.send(signedTx)
    }
}

// ---------------------------------------------------------------------------------------------------------------------

@InitiatingFlow
@StartableByRPC
class RequestCollectBlockSignaturesFlow(
    private val transactionId: SecureHash,
    private val blockNumber: BigInteger,
    private val blocking: Boolean,
    private val targetParty: Party
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        if (ourIdentity == targetParty) {
            // Directly call CollectBlockSignaturesFlow if on targetParty's node
            subFlow(CollectBlockSignaturesFlow(transactionId, blockNumber, blocking))
        } else {
            // Start a session with targetParty and send necessary info
            val session = initiateFlow(targetParty)
            session.send(transactionId)
            session.send(blockNumber)
            session.send(blocking)

            if(blocking) session.receive<Boolean>()
        }
    }
}

@InitiatedBy(RequestCollectBlockSignaturesFlow::class)
class RespondToCollectBlockSignaturesFlow(val session: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val transactionId = session.receive<SecureHash>().unwrap { it }
        val blockNumber = session.receive<BigInteger>().unwrap { it }
        val blocking = session.receive<Boolean>().unwrap { it }

        // Now, execute the CollectBlockSignaturesFlow with the received parameters
        subFlow(CollectBlockSignaturesFlow(transactionId, blockNumber, blocking))

        // If the original flow is blocking, send a dummy confirmation back
        if (blocking) {
            session.send(true)
        }
    }
}


// ---------------------------------------------------------------------------------------------------------------------

@InitiatingFlow
@StartableByRPC
class RequestUnlockAssetFlow(
    private val transactionId: SecureHash,
    private val blockNumber: BigInteger,
    private val transactionIndex: BigInteger,
    private val targetParty: Party
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return if (ourIdentity == targetParty) {
            // Directly call UnlockAssetFlow if on targetParty's node
            subFlow(UnlockAssetFlow(transactionId, blockNumber, transactionIndex))
        } else {
            // Start a session with targetParty and send necessary info
            val session = initiateFlow(targetParty)
            session.send(transactionId)
            session.send(blockNumber)
            session.send(transactionIndex)
            // Receive the SignedTransaction response from targetParty
            return session.receive<SignedTransaction>().unwrap { it }
        }
    }
}

@InitiatedBy(RequestUnlockAssetFlow::class)
class RespondToUnlockAssetFlow(private val session: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val transactionId = session.receive<SecureHash>().unwrap { it }
        val blockNumber = session.receive<BigInteger>().unwrap { it }
        val transactionIndex = session.receive<BigInteger>().unwrap { it }

        // Execute the UnlockAssetFlow with received parameters
        val signedTx = subFlow(UnlockAssetFlow(transactionId, blockNumber, transactionIndex))
        // Send back the result
        session.send(signedTx)
    }
}

// ---------------------------------------------------------------------------------------------------------------------

@InitiatingFlow
@StartableByRPC
class RequestCollectNotarizationSignaturesFlow(
    private val transactionId: SecureHash,
    private val blocking: Boolean,
    private val targetParty: Party
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        if (ourIdentity == targetParty) {
            // Directly call CollectNotarizationSignaturesFlow if on targetParty's node
            subFlow(CollectNotarizationSignaturesFlow(transactionId, blocking))
        } else {
            // Start a session with targetParty and send necessary info
            val session = initiateFlow(targetParty)
            session.send(transactionId)
            session.send(blocking)
        }
    }
}

@InitiatedBy(RequestCollectNotarizationSignaturesFlow::class)
class RespondToCollectNotarizationSignaturesFlow(private val session: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Receive the parameters sent by the initiating flow
        val transactionId = session.receive<SecureHash>().unwrap { it }
        val blocking = session.receive<Boolean>().unwrap { it }

        // Now, execute the CollectNotarizationSignaturesFlow with the received parameters
        subFlow(CollectNotarizationSignaturesFlow(transactionId, blocking))
    }
}

// ---------------------------------------------------------------------------------------------------------------------

@InitiatingFlow
@StartableByRPC
class RequestCommitWithTokenFlow(
    private val transactionId: SecureHash,
    private val tokenAddress: String,
    private val tokenId: BigInteger,
    private val amount: BigInteger,
    private val recipient: String,
    private val signaturesThreshold: BigInteger,
    private val signers: List<String>,
    private val targetParty: Party
) : FlowLogic<TransactionReceipt>() {


    constructor(
        transactionId: SecureHash,
        tokenAddress: String,
        amount: BigInteger,
        recipient: String,
        signaturesThreshold: BigInteger,
        targetParty: Party
    ) : this(transactionId, tokenAddress, BigInteger.ZERO, amount, recipient, signaturesThreshold, emptyList(), targetParty)

    constructor(
        transactionId: SecureHash,
        tokenAddress: String,
        amount: BigInteger,
        recipient: String,
        signaturesThreshold: BigInteger,
        signers: List<String>,
        targetParty: Party
    ) : this(transactionId, tokenAddress, BigInteger.ZERO, amount, recipient, signaturesThreshold, signers, targetParty)

    @Suspendable
    override fun call(): TransactionReceipt {
        return if (ourIdentity == targetParty) {
            // Directly call CommitWithTokenFlow if on targetParty's node
            subFlow(CommitWithTokenFlow(transactionId, tokenAddress, tokenId, amount, recipient, signaturesThreshold, signers))
        } else {
            // Start a session with targetParty and send necessary info
            val session = initiateFlow(targetParty)
            session.send(transactionId)
            session.send(tokenAddress)
            session.send(tokenId)
            session.send(amount)
            session.send(recipient)
            session.send(signaturesThreshold)
            session.send(signers)
            // Receive the TransactionReceipt response from targetParty
            return session.receive<TransactionReceipt>().unwrap { it }
        }
    }
}

@InitiatedBy(RequestCommitWithTokenFlow::class)
class RespondToCommitWithTokenFlow(private val session: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val transactionId = session.receive<SecureHash>().unwrap { it }
        val tokenAddress = session.receive<String>().unwrap { it }
        val tokenId = session.receive<BigInteger>().unwrap { it }
        val amount = session.receive<BigInteger>().unwrap { it }
        val recipient = session.receive<String>().unwrap { it }
        val signaturesThreshold = session.receive<BigInteger>().unwrap { it }
        val signers = session.receive<List<String>>().unwrap { it }

        // Execute the CommitWithTokenFlow with received parameters
        val txReceipt = subFlow(CommitWithTokenFlow(transactionId, tokenAddress, tokenId, amount, recipient, signaturesThreshold, signers))
        // Send back the result
        session.send(txReceipt)
    }
}

// ---------------------------------------------------------------------------------------------------------------------

@InitiatingFlow
@StartableByRPC
class RequestClaimCommitmentWithSignaturesFlow(
    private val transactionId: SecureHash,
    private val signatures: List<ByteArray>,
    private val targetParty: Party
) : FlowLogic<TransactionReceipt>() {
    @Suspendable
    override fun call(): TransactionReceipt {
        return if (ourIdentity == targetParty) {
            subFlow(ClaimCommitmentWithSignatures(transactionId, signatures))
        } else {
            val session = initiateFlow(targetParty)
            session.send(transactionId)
            session.send(signatures)
            return session.receive<TransactionReceipt>().unwrap { it }
        }
    }
}

@InitiatedBy(RequestClaimCommitmentWithSignaturesFlow::class)
class RespondToClaimCommitmentWithSignaturesFlow(private val session: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val transactionId = session.receive<SecureHash>().unwrap { it }
        val signatures = session.receive<List<ByteArray>>().unwrap { it }
        val txReceipt = subFlow(ClaimCommitmentWithSignatures(transactionId, signatures))
        session.send(txReceipt)
    }
}

// ---------------------------------------------------------------------------------------------------------------------

@InitiatingFlow
@StartableByRPC
class RequestGetBlockReceiptsFlow(
    private val blockNumber: BigInteger,
    private val targetParty: Party
) : FlowLogic<List<com.r3.corda.evminterop.dto.TransactionReceipt>>() {
    @Suspendable
    override fun call(): List<com.r3.corda.evminterop.dto.TransactionReceipt> {
        return if (ourIdentity == targetParty) {
            subFlow(GetBlockReceiptsFlow(blockNumber))
        } else {
            val session = initiateFlow(targetParty)
            session.send(blockNumber)
            return session.receive<List<com.r3.corda.evminterop.dto.TransactionReceipt>>().unwrap { it }
        }
    }
}

@InitiatedBy(RequestGetBlockReceiptsFlow::class)
class RespondToGetBlockReceiptsFlow(private val session: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val blockNumber = session.receive<BigInteger>().unwrap { it }
        val receipts = subFlow(GetBlockReceiptsFlow(blockNumber))
        session.send(receipts)
    }
}

// ---------------------------------------------------------------------------------------------------------------------

@InitiatingFlow
@StartableByRPC
class RequestClaimCommitmentFlow(
    private val transactionId: SecureHash,
    private val targetParty: Party
) : FlowLogic<TransactionReceipt>() {
    @Suspendable
    override fun call(): TransactionReceipt {
        return if (ourIdentity == targetParty) {
            subFlow(ClaimCommitment(transactionId))
        } else {
            val session = initiateFlow(targetParty)
            session.send(transactionId)
            return session.receive<TransactionReceipt>().unwrap { it }
        }
    }
}

@InitiatedBy(RequestClaimCommitmentFlow::class)
class RequestClaimCommitmentFlowResponder(
    val session: FlowSession
) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val transactionId = session.receive<SecureHash>().unwrap { it }
        val receipt = subFlow(ClaimCommitment(transactionId))
        session.send(receipt)
    }
}
