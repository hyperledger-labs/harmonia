package com.r3.corda.template.webserver.example

import com.fasterxml.jackson.databind.ObjectMapper
import com.interop.flows.CollectBlockSignaturesFlow
import com.r3.corda.evminterop.workflows.GenericAssetState
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import javax.servlet.http.HttpServletRequest
import com.r3.corda.evminterop.workflows.IssueGenericAssetFlow
import com.interop.flows.UnlockAssetFlow

import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.node.services.vault.QueryCriteria
import com.interop.flows.DraftAssetSwapBaseFlow
import com.interop.flows.SignDraftTransactionByIDFlow
import com.r3.corda.evminterop.Erc20TransferEventEncoder
import com.r3.corda.evminterop.dto.TransactionReceipt
import com.r3.corda.evminterop.workflows.eth2eth.Erc20TransferFlow
import org.springframework.web.bind.annotation.*


data class AssetReturnType(
    val assetName : String,
    val txHash : String,
    val index : Int
)
@CrossOrigin(origins = ["http://localhost:3000"])
@RestController
@RequestMapping("/")
class Controller(rpc: InteropNodeRpcConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = ["me"], produces = ["application/json"])
    fun whoami() = mapOf("me" to "Corda")



    @GetMapping(value = ["all-assets"], produces = ["application/json"])
    fun allAssets(): ResponseEntity<List<AssetReturnType>> {
        val assets = proxy.vaultQuery(GenericAssetState::class.java).states
        return ResponseEntity.ok(assets.map {
            AssetReturnType(it.state.data.assetName, it.ref.txhash.toString(), it.ref.index)
        })
    }
    // Post mapping that generates a generic asset on corda
    @PostMapping(value = ["generate-asset"], produces = ["application/json"])
    fun createGenericAsset(request: HttpServletRequest): ResponseEntity<String> {
        // get body of the request
        val body = request.reader.readText()
        println("Request body: $body")

        // Parse JSON body using Jackson
        val mapper = ObjectMapper()
        val jsonNode = mapper.readTree(body)
        val assetName = jsonNode.get("assetName")?.asText()
            ?: return ResponseEntity.badRequest().body("assetName is required")
        println("Asset name: $assetName")
        val output = proxy.startFlow(::IssueGenericAssetFlow, assetName).returnValue.get()
        println("Output: $output")
        return ResponseEntity.ok(output.toString())
    }

    @PostMapping(value = ["draft-asset-swap"], produces = ["application/json"])
    fun createDraftAssetSwap(request: HttpServletRequest): ResponseEntity<String> {
        // query the asseti
        // create a draft asset swap
        // build a criteria builder

        val body = request.reader.readText()
        val mapper = ObjectMapper()
        val jsonNode = mapper.readTree(body)
        val assetTxhash = jsonNode.get("txHash")?.asText()
        val id = SecureHash.parse(assetTxhash)
        val index = jsonNode.get("txIndex")?.asInt() ?: 0


        val recipientX500Name =
            jsonNode.get("recipient")?.asText() ?: return ResponseEntity.badRequest().body("Recipient is required")
        val recipientParty = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(recipientX500Name))
            ?: return ResponseEntity.badRequest().body("Recipient not found")

        // validators as a list
        val validators = jsonNode.get("validators")?.map {
            proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(it.asText()))
                ?: return ResponseEntity.badRequest().body("Validator not found")
        } ?: return ResponseEntity.badRequest().body("Validators are required")

        val signaturesRequired = jsonNode.get("signaturesRequired")?.asInt() ?: return ResponseEntity.badRequest()
            .body("Signatures required is required")

        val contractAddress = jsonNode.get("contractAddress")?.asText() ?: return ResponseEntity.badRequest()
            .body("Contract address is required")
        val fromAddress =
            jsonNode.get("fromAddress")?.asText() ?: return ResponseEntity.badRequest().body("From address is required")
        val toAddress =
            jsonNode.get("toAddress")?.asText() ?: return ResponseEntity.badRequest().body("To address is required")
        val evmAmount =
            jsonNode.get("evmAmount")?.asText() ?: return ResponseEntity.badRequest().body("EVM amount is required")

        val transferEventEncoder =
            Erc20TransferEventEncoder(contractAddress, fromAddress, toAddress, evmAmount.toBigInteger())


        val stateRef = StateRef(id, index)

        val criteria = QueryCriteria.VaultQueryCriteria()
            .withStateRefs(listOf(stateRef))  // Assuming output index 0, adjust if needed

        val results = proxy.vaultQueryByCriteria(criteria, GenericAssetState::class.java)
        if (results.states.isEmpty()) {
            return ResponseEntity.badRequest().body("Asset not found")
        }
        // get the fist
        val draftTxHash = proxy.startFlow(
            ::DraftAssetSwapBaseFlow,
            stateRef,
            recipientParty,
            proxy.notaryIdentities().first(),
            validators,
            signaturesRequired,
            transferEventEncoder
        ).returnValue.get()

        return ResponseEntity.ok(draftTxHash.toString())
    }

    @PostMapping(value=["sign-draft-transaction"], produces = ["application/json"])
    fun signDraftTransaction(request: HttpServletRequest): ResponseEntity<String> {
        val body = request.reader.readText()
        val mapper = ObjectMapper()
        val jsonNode = mapper.readTree(body)
        val draftTxHash = jsonNode.get("draftTxHash")?.asText()
            ?: return ResponseEntity.badRequest().body("Draft transaction hash is required")
        val signedDraftTransaction = proxy.startFlow(::SignDraftTransactionByIDFlow, SecureHash.parse(draftTxHash)).returnValue.get()
        return ResponseEntity.ok(signedDraftTransaction.tx.id.toString())
    }

    // TODO: Improve responses
    @PostMapping(value=["transfer-and-prove"], produces = ["application/json"])
    fun transferAndProve(request: HttpServletRequest): ResponseEntity<TransactionReceipt?> {
        // get the body of the request
        val body = request.reader.readText()
        val mapper = ObjectMapper()
        val jsonNode = mapper.readTree(body)
        val amount = jsonNode.get("amount")?.asText()
            ?: return ResponseEntity.badRequest().body(null)
        val toAddress = jsonNode.get("toAddress")?.asText()
            ?: return ResponseEntity.badRequest().body(null)
        val contractAddress = jsonNode.get("contractAddress")?.asText()
            ?: return ResponseEntity.badRequest().body(null)
        val transactionReceipt = proxy.startFlow(::Erc20TransferFlow, contractAddress, toAddress, amount.toBigInteger()).returnValue.get()
        return ResponseEntity.ok(transactionReceipt)
    }
    // Query the vault for the generic asset

    @PostMapping(value= ["collect-block-signatures"], produces = ["application/json"])
    fun collectBlockSignatures(request: HttpServletRequest): ResponseEntity<String> {
        val body = request.reader.readText()
        val mapper = ObjectMapper()
        val jsonNode = mapper.readTree(body)
        val draftTxHash = jsonNode.get("draftTxHash")?.asText()
            ?: return ResponseEntity.badRequest().body("Draft transaction hash is required")
        val blockNumber = jsonNode.get("blockNumber")?.asInt()
            ?: return ResponseEntity.badRequest().body("Block number is required")
        val blocking = jsonNode.get("blocking")?.asBoolean()
            ?: return ResponseEntity.badRequest().body("isFinal is required")
        proxy.startFlow(::CollectBlockSignaturesFlow, SecureHash.parse(draftTxHash), blockNumber.toBigInteger(), blocking)
        return ResponseEntity.ok("Block signatures collected")
    }

    @PostMapping(value=["unlock-asset"], produces = ["application/json"])
    fun unlockAssetFlow(request: HttpServletRequest): ResponseEntity<String> {
        val body = request.reader.readText()
        val mapper = ObjectMapper()
        val jsonNode = mapper.readTree(body)
        val signedTransactionId = jsonNode.get("signedTransactionId")?.asText()
            ?: return ResponseEntity.badRequest().body("Draft transaction hash is required")
        val blockNumber = jsonNode.get("blockNumber")?.asInt()
            ?: return ResponseEntity.badRequest().body("Block number is required")
        val transactionIndex = jsonNode.get("transactionIndex")?.asInt()
            ?: return ResponseEntity.badRequest().body("Transaction index is required")
        val unlockTx = proxy.startFlow(::UnlockAssetFlow, SecureHash.parse(signedTransactionId), blockNumber.toBigInteger(), transactionIndex.toBigInteger()).returnValue.get()
        return ResponseEntity.ok(unlockTx.toString())
    }



}