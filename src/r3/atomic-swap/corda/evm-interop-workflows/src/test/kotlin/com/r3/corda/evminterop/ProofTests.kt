package com.r3.corda.evminterop

import com.r3.corda.evminterop.dto.TransactionReceipt
import com.r3.corda.evminterop.dto.encoded
import com.r3.corda.evminterop.internal.TestNetSetup
import com.r3.corda.evminterop.services.swap.DraftTxService
import com.r3.corda.evminterop.states.swap.SwapTransactionDetails
import com.r3.corda.evminterop.workflows.GenericAssetState
import com.r3.corda.evminterop.workflows.IssueGenericAssetFlow
import com.r3.corda.evminterop.workflows.eth2eth.Erc20TransferFlow
import com.r3.corda.evminterop.workflows.eth2eth.GetBlockFlow
import com.r3.corda.evminterop.workflows.eth2eth.GetBlockReceiptsFlow
import com.r3.corda.evminterop.workflows.eth2eth.GetTransactionFlow
import com.r3.corda.evminterop.workflows.swap.BuildAndProposeDraftTransactionFlow
import com.r3.corda.evminterop.workflows.swap.RequestNotarizationProofsInitiator
import com.r3.corda.evminterop.workflows.swap.SignDraftTransactionFlow
import com.r3.corda.interop.evm.common.trie.PatriciaTrie
import net.corda.core.utilities.getOrThrow
import org.junit.Assert
import org.junit.Test
import org.web3j.abi.datatypes.Address
import org.web3j.crypto.Hash
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpString
import org.web3j.utils.Numeric
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class ProofTests : TestNetSetup() {

    @Test
    fun `can prove inclusion of transaction in a block`() {
        val amount = 1.toBigInteger()
        val eventSignature = "Transfer(address,address,uint256)"
        val signatureHash: String = Numeric.toHexString(Hash.sha3(eventSignature.toByteArray()))

        // create an ERC20 Transaction that will emit a Transfer event
        val transactionReceipt: TransactionReceipt = alice.startFlow(
            Erc20TransferFlow(goldTokenDeployAddress, bobAddress, amount)
        ).getOrThrow()

        // Retrieve the event from the receipt logs. The event will have references to the
        // transaction that generated it, and the block that mined the transaction.
        val transferEvent = transactionReceipt.logs?.single { log ->
            // Expecting an event with 3 Topics
            log.topics != null && log.topics!!.size == 3
            // Topic0 = event signature
            log.topics!![0] == signatureHash &&
            // Topic1 = address indexed `Transfer from`
            log.topics!![1].takeLast(40) == Numeric.cleanHexPrefix(aliceAddress) &&
            // Topic2 = address indexed `Transfer to`
            log.topics!![2].takeLast(40) == Numeric.cleanHexPrefix(bobAddress) &&
            // data = uint256 non-indexed `Transfer amount` - single non-indexed value = 32 bytes long string
            log.data != null && Numeric.toBigInt(log.data) == amount
        }
        assertNotNull(transferEvent, "The expected Transfer event was not found")

        // Got the event from the transaction receipt instead that from event filter
        // Same logic applies after proving the transaction contains the expected event

        // get the transaction that generated the event following the transactionHash from the event
        val eventSourceTx = alice.startFlow(
           GetTransactionFlow(hash = transferEvent?.transactionHash!!)
        ).getOrThrow()

        // get the block that mined the transaction that generated the event following the blockHash from the event
        val eventSourceBlock = alice.startFlow(
            GetBlockFlow(hash = transferEvent.blockHash!!, includeTransactions = true)
        ).getOrThrow()

        // make sure the block contains the transaction that generated the event
        eventSourceBlock.transactions.filter { it.hash == eventSourceTx.hash }.singleOrNull {
            it.hash == Hash.sha3(it.raw) && it.hash == Hash.sha3(eventSourceTx.raw)
        }

        // make sure the block is not tampered with by calculating the transactions trie root
        // and comparing it against the block-header's transactionsRoot
        val trie = PatriciaTrie()
        for(tx in eventSourceBlock.transactions) {
            trie.put(
                RlpEncoder.encode(RlpString.create(tx.transactionIndex.toLong())),
                Numeric.hexStringToByteArray(tx.raw)
            )
        }
        assertEquals(eventSourceBlock.transactionsRoot, Numeric.toHexString(trie.root.hash))
    }

    @Test
    fun `can prove inclusion of event in a block`() {

        // Event expectations are defined ahead of the transaction/event
        val encodedEvent = DefaultEventEncoder.encodeEvent(
            goldTokenDeployAddress,
            "Transfer(address,address,uint256)",
            Indexed(aliceAddress),
            Indexed(bobAddress),
            1.toBigInteger()
        )

        val amount = 1.toBigInteger()
        // create an ERC20 Transaction that will emit a Transfer event
        val transactionReceipt: TransactionReceipt = alice.startFlow(
            Erc20TransferFlow(goldTokenDeployAddress, bobAddress, amount)
        ).getOrThrow()

        val transferEvent = encodedEvent.findIn(transactionReceipt)
        assertNotNull(transferEvent.found, "The expected Transfer event was not found")

        // Got the event from the transaction receipt instead that from event filter
        // Same logic applies after proving the transaction contains the expected event

        // get the block that mined the transaction that generated the event following the blockHash from the event
        val eventSourceBlock = alice.startFlow(
            GetBlockFlow(hash = transferEvent.log.blockHash!!, includeTransactions = true)
        ).getOrThrow()

        val receipts = alice.startFlow(
            GetBlockReceiptsFlow(eventSourceBlock.number)
        ).getOrThrow()

        // make sure the block is not tampered with by calculating the transactions trie root
        // and comparing it against the block-header's transactionsRoot
        val trie = PatriciaTrie()
        for(receipt in receipts) {
            trie.put(
                RlpEncoder.encode(RlpString.create(Numeric.toBigInt(receipt.transactionIndex!!).toLong())),
                receipt.encoded()
            )
        }
        assertEquals(eventSourceBlock.receiptsRoot, Numeric.toHexString(trie.root.hash))

        val txIndex = Numeric.toBigInt(transferEvent.log.transactionIndex!!).toInt()
        val key = RlpEncoder.encode(RlpString.create(txIndex.toLong()))
        val transactionProof = trie.generateMerkleProof(key)

        val verified = PatriciaTrie.verifyMerkleProof(
            Numeric.hexStringToByteArray(eventSourceBlock.receiptsRoot),
            key,
            receipts[txIndex].encoded(),
            transactionProof
        )
        assertTrue(verified)
    }

    @Test
    fun `can prove notarisation of transaction with evm signature`() {

        val assetName = UUID.randomUUID().toString()

        // Create Corda asset owned by Bob
        val assetTx = await(bob.startFlow(IssueGenericAssetFlow(assetName)))

        val asset =
            bob.services.vaultService.queryBy(GenericAssetState::class.java, queryCriteria(assetName)).states.single()

        // Generate swap transaction details. These details are shared by both swap parties and are used to coordinate
        // and identify events
        val swapDetails = SwapTransactionDetails(
            senderCordaName = bob.toParty(),
            receiverCordaName = alice.toParty(),
            cordaAssetState = asset,
            approvedCordaValidators = listOf(charlie.toParty()),
            minimumNumberOfEventValidations = 1,
            unlockEvent = transferEventEncoder
        )

        // Build draft transaction and send it to counterparty for verification
        val draftTx = await(bob.startFlow(BuildAndProposeDraftTransactionFlow(swapDetails, notary.toParty())))
        Assert.assertEquals(
            draftTx!!.id,
            alice.services.cordaService(DraftTxService::class.java).getDraftTx(draftTx.id)!!.id
        )
        Assert.assertEquals(
            draftTx!!.id,
            bob.services.cordaService(DraftTxService::class.java).getDraftTx(draftTx.id)!!.id
        )

        // We generate an EVM asset transfer on EVM from Alice to Bob and retrieve the transaction receipt with the event
        // logs, and generate a merkle-proof form it (includes the proof's leaf key).
        val (txReceipt, leafKey, merkleProof) = transferAndProve(1.toBigInteger(), alice, bobAddress)

        // Bob receives the receipt/event confirming the EVM asset transfer, so it is safe to sign because the event can
        // be used to unlock the asset.
        val signedTx = await(bob.startFlow(SignDraftTransactionFlow(draftTx)))

        val signatures = await(bob.startFlow(RequestNotarizationProofsInitiator(signedTx.tx.id, listOf(charlie.toParty()))))

        val signatureBytes = assertNotNull(signatures.singleOrNull())

        // Convert the signature bytes to a Sign.SignatureData object
        val signatureData = Sign.SignatureData(
            signatureBytes[64], // V
            signatureBytes.copyOfRange(0, 32), // R
            signatureBytes.copyOfRange(32, 64)  // S
        )

        val signedData = signedTx.tx.id.bytes

        // Verify the signature and get the signer's address
        val publicKey = Sign.signedMessageToKey(signedData, signatureData)
        val signerAddress = Keys.getAddress(publicKey)

        assertEquals(Address(signerAddress), Address(charlieAddress))
    }
}
