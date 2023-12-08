package com.r3.corda.evminterop.services

import com.r3.corda.evminterop.dto.Block
import com.r3.corda.evminterop.dto.TransactionReceipt
import com.r3.corda.evminterop.workflows.internal.toSerializable
import net.corda.core.flows.FlowExternalOperation
import net.corda.core.utilities.loggerFor
import org.web3j.crypto.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.Web3jService
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.RemoteFunctionCall
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.response.*
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*
import java.util.concurrent.*
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.Pair
import kotlin.concurrent.timer

/**
 * Represents a unique connection to a EVM based blockchain network that can be shared by multiple [Session]s.
 * The [Session] identifies the [Connection] by the [ConnectionId].
 */
internal class Connection(private val connectionId: ConnectionId) {

    companion object {
        /**
         * Provides a fixed pool of threads to handle network calls from the queue. The servicing of the network/ethereum
         * calls needs a review and a fixed pool of threads may not be the best solution.
         */
        private val executors: ExecutorService = Executors.newCachedThreadPool()
        private val pollingExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

        /**
         * Logger instance for [Connection]
         */
        private val log = loggerFor<Connection>()

        /**
         * The maximum number of transactions that are being polled for status at any time
         */
        private val pollBatchSize = 4
    }

    /**
     * [chainId] of the selected EVM network
     */
    val chainId: Long = 1337

    /**
     * Web3j high level network connection that provides APIs for java smart contract wrappers generated via Web3j
     * plugin from the source solidity smart contracts (so far ERC20 and HtlcBridge).
     */
    internal val web3j: Web3j by lazy { initWeb3jConnection(web3jService) }

    /**
     * Web3jService abstracts underlying Web3j connection (http, https, ws, wss)
     */
    private val web3jService: Web3jService by lazy { initWeb3jService() }

    /**
     * A queue to receive future/completable remote network calls.
     * Ethereum network calls are queued here for the polling module to dequeue and check the transaction status.
     */
    private val queue = LinkedList<CompletableTransaction>()

    /**
     * Initializes Web3j underlying network connection depending on the configuration which is currently provided
     * by the simplistic KnownNetworks.
     */
    private fun initWeb3jService(): Web3jService {
        return if (connectionId.isHttp) {
            httpConnection()
        } else if (connectionId.isWebsocket) {
            throw NotImplementedError("Websocket not supported")
        } else throw IllegalArgumentException(
            "Could not determine connection type of the current network (${connectionId.rpcEndpoint})"
        )
    }

    /**
     * Initializes Web3j high level connection and registers Ethereym's BLOCK and TRANSACTION event listeners.
     * @param connection receives the abstract underlying network connection
     */
    private fun initWeb3jConnection(connection: Web3jService): Web3j {
        // NOTE: web3j.ethChainId() could be used to identify network and set associated confirmation blocks nr.
        return Web3j.build(connection)
    }


    /**
     * Initializes the HTTP/S connection that servers as the Web3j underlying network connection
     */
    private fun httpConnection(): Web3jService {
        try {
            return HttpService(connectionId.rpcEndpoint.toURL().toString())
        } finally {
            pollingExecutor.scheduleAtFixedRate({ poll() }, 0, 5, TimeUnit.SECONDS)
        }
    }

    private val lock = Any()
    
    /**
     * Implements a polling module to query ethereum calls that are pending response form the network.
     */
    private fun poll() {
        val items = synchronized(lock) {
            if (queue.isEmpty()) return

            val expired = queue.filter { it.isExpired }
            val complete = queue.filter { it.isComplete }

            queue.removeAll((expired + complete).toSet())

            val inFlight = queue.count { it.inFlight }
            val batch = queue.filter { !it.inFlight }.take(pollBatchSize - inFlight)
            Pair(expired, batch)
        }

        try {
            items.first.forEach { it.completeWithTimeout() }
        } catch (e: Exception) {
            log.error("Error signalling transaction timeouts: $e")
        }

        items.second.map { item ->
            item.inFlight = true
            val futures = CompletableFuture.supplyAsync(
                Supplier<EthGetTransactionReceipt> {
                    try {
                        web3j.ethGetTransactionReceipt(item.transactionHash).send()
                    } catch (e: Exception) {
                        item.inFlight = false
                        log.error("Error queueing transaction timeouts: $e")
                        EthGetTransactionReceipt()
                    }
                }, executors
            ).thenAcceptAsync(
                Consumer<EthGetTransactionReceipt> { it ->
                    if (it.result != null) {
                        try {
                            val receipt = it.result.toSerializable()
                            item.complete(receipt)
                        } catch (e: Throwable) {
                            item.completeError("${e.message}")
                        }
                    } else if (it.error != null) {
                        item.completeError("${it.error.message} (${it.error.code})")
                    } else {
                        item.completeError("No result/response received")
                    }
                }, executors
            )
        }
    }
    /**
     * Implements the transaction status query from the Ethereum network used by the polling module
     */
    private fun pollTransaction(transaction: CompletableTransaction) {
        transaction.inFlight = true
        try {
            web3j.ethGetTransactionReceipt(transaction.transactionHash).sendAsync().thenApply {
                if (it.result != null) {
                    try {
                        val receipt: TransactionReceipt = it.result.toSerializable()
                        transaction.complete(receipt)
                    } catch(e: Throwable) {
                        transaction.completeError("${e.message}")
                    }
                } else if (it.error != null) {
                    transaction.completeError("${it.error.message} (${it.error.code})")
                } else {
                    transaction.completeError("No result/response received")
                }
            }
        } catch (e: Exception) {
            transaction.inFlight = false
            log.error("Error queueing transaction timeouts: $e")
        }
    }

    /**
     * Enqueues a non-transactional Web3j remote function call and translates the async [CompletableFuture]
     * into a Corda awaitable [FlowExternalOperation]
     */
    fun<T> simpleRemoteFunctionCall(fn: RemoteFunctionCall<T>): ResponseOperation<T> where  T : Any {
        val response = fn.sendAsync()
        return ResponseOperation(response)
    }

    /**
     * Enqueues a non-transactional Web3j remote function call and translates the async [CompletableFuture]
     * into a Corda awaitable [FlowExternalOperation]. The [transform] parameter allows to transform the
     * future response.
     */
    fun<T,R> simpleRemoteFunctionCall(fn: RemoteFunctionCall<T>, transform: (T) -> R):
            ResponseOperation<R> where  T : Any, R: Any {
        val response = fn.sendAsync().thenApply {  transform(it)  }
        return ResponseOperation(response)
    }

    /**
     * Enqueues a transactional Web3j remote function call and transforms the async [CompletableFuture] of type
     * [TransactionReceipt] into a Corda awaitable [FlowExternalOperation], [CordaSerializable] version of the
     * [TransactionReceipt].
     */
    fun queueRemoteFunctionCall(fn: RemoteFunctionCall<org.web3j.protocol.core.methods.response.TransactionReceipt>): ResponseOperation<TransactionReceipt> {
        val response = fn.sendAsync().thenCompose {
            queueTransactionReceiptResponse(it.transactionHash)
        }
        return ResponseOperation(response)
    }

    private fun queueTransactionReceiptResponse(hash: String): CompletableFuture<TransactionReceipt> {
        val future = CompletableFuture<TransactionReceipt>()
        synchronized(lock) {
            queue.add(CompletableTransaction(hash, future))
        }
        return future
    }

    fun queueEventLogResponse(address: String): ResponseOperation<com.r3.corda.evminterop.dto.TransactionReceiptLog> {
        throw NotImplementedError()
//        val future = CompletableFuture<com.r3.corda.evminterop.dto.TransactionReceiptLog>()
//
//        var requireRegister = false
//        eq.getOrPut(address) {
//            requireRegister = true
//            ConcurrentLinkedQueue()
//        }.add(CompletableEvent(future))
//
//        if (requireRegister) {
//            registerFilter(address)
//        }
//
//        return ResponseOperation(future)
    }

    private fun registerFilter(address: String) {
        throw NotImplementedError()
//        val filter = EthFilter(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST, address)
//        web3j.ethLogFlowable(filter).subscribe { log ->
//            eq[address]!!.map { completableEvent ->
//                completableEvent.complete(
//                    com.r3.corda.evminterop.dto.TransactionReceiptLog(
//                        removed = log.isRemoved,
//                        logIndex = log.logIndexRaw,
//                        transactionIndex = log.transactionIndexRaw,
//                        transactionHash = log.transactionHash,
//                        blockHash = log.blockHash,
//                        blockNumber = log.blockNumber,
//                        address = log.address,
//                        data = log.data,
//                        type = log.type,
//                        topics = log.topics
//                    )
//                )
//            }
//        }
    }

    /**
     * Implements raw EVM call with method name and parameters
     */
    fun <T> ethCall(method: String, params: List<T>): CompletableFuture<EthCall> {
        return Request(
            method,
            params,
            web3jService,
            EthCall::class.java
        ).sendAsync()
    }

    /**
     * Query the balance of the native chain token for the account
     */
    fun getBalance(address: String, blockParameterName: DefaultBlockParameter): CompletableFuture<EthGetBalance> {
        return web3j.ethGetBalance(address, blockParameterName).sendAsync()
    }

    /**
     * Query a block by its number allowing to choose between full transaction objects or hashes
     */
    fun getBlockByNumber(number: BigInteger, fullTransactionObject: Boolean): CompletableFuture<Block> {
        val response = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(number), fullTransactionObject).sendAsync()

        return response.thenApply {
            if (fullTransactionObject) {
                for (transaction in it.result.transactions) {
                    rebuildRawTransaction(transaction as EthBlock.TransactionObject)
                }
            }
            mapBlock(it)
        }
    }

    /**
     * Query a block by its hash allowing to choose between full transaction objects or hashes
     */
    fun getBlockByHash(hash: String, fullTransactionObject: Boolean): CompletableFuture<Block> {
        val response = web3j.ethGetBlockByHash(hash, fullTransactionObject).sendAsync()

        return response.thenApply {
            if (fullTransactionObject) {
                for (transaction in it.result.transactions) {
                    rebuildRawTransaction(transaction as EthBlock.TransactionObject)
                }
            }
            mapBlock(it)
        }
    }

    private fun mapBlock(ethBlock: EthBlock) : Block {
        val block = ethBlock.block
        return Block(
            block.number,
            block.hash,
            block.parentHash,
            block.nonce,
            block.sha3Uncles,
            block.logsBloom,
            block.transactionsRoot,
            block.stateRoot,
            block.receiptsRoot,
            //try { block.author } catch(e: Throwable) { "" },
            block.miner,
            block.mixHash,
            block.difficulty,
            block.totalDifficulty,
            block.extraData,
            block.size,
            block.gasLimit,
            block.gasUsed,
            block.timestamp,
            block.transactions.map {
                // NOTE: when fullTransactionObject = false, the TransactionResult is the hash (String) of the
                //       transaction, not the transaction object! Implement support for both cases.
                mapTransaction(it as EthBlock.TransactionObject)
            },
            block.uncles,
            //block.sealFields,
            block.baseFeePerGas
        )
    }

    fun mapTransaction(tx: Transaction): com.r3.corda.evminterop.dto.Transaction {
        //val tx = ethTransaction.transaction.orElseThrow { IllegalArgumentException("Null transaction!") }
        return com.r3.corda.evminterop.dto.Transaction(
            tx.hash,
            tx.nonce,
            tx.blockHash,
            tx.blockNumber,
            tx.chainId,
            tx.transactionIndex,
            tx.from,
            tx.to,
            tx.value,
            tx.gasPrice,
            tx.gas,
            tx.input,
            tx.raw,
            tx.r,
            tx.s,
            tx.v,
            tx.type,
            if(tx.maxFeePerGasRaw == null) BigInteger.ZERO else tx.maxFeePerGas,
            if(tx.maxPriorityFeePerGasRaw == null) BigInteger.ZERO else tx.maxPriorityFeePerGas
        )
    }

    fun mapTransactionReceipt(receipt: org.web3j.protocol.core.methods.response.TransactionReceipt): com.r3.corda.evminterop.dto.TransactionReceipt {
        return receipt.toSerializable()
    }

    fun getTransactionByHash(hash: String): CompletableFuture<com.r3.corda.evminterop.dto.Transaction> {
        val txResponse = web3j.ethGetTransactionByHash(hash).sendAsync()

        return txResponse.thenApply {
            val tx = rebuildRawTransaction(it.result)
            mapTransaction(it.result)
        }
    }

    fun getTransactionReceiptByHash(hash: String): CompletableFuture<com.r3.corda.evminterop.dto.TransactionReceipt> {
        val txResponse = web3j.ethGetTransactionReceipt(hash).sendAsync()

        return txResponse.thenApply {
            mapTransactionReceipt(it.result)
        }
    }

    fun getBlockReceipts(blockNumber: BigInteger) : CompletableFuture<List<TransactionReceipt>> {
        TODO("Depends on web3j 4.10.1-CORDA4 fork, which is soon to be published on the R3 Artifactory.")
//        val txResponse = web3j.ethGetBlockReceipts(DefaultBlockParameter.valueOf(blockNumber)).sendAsync()
//
//        return txResponse.thenApply { it ->
//            val blockReceipts = it.blockReceipts
//            if(blockReceipts.isPresent) blockReceipts.get().map { receipt ->
//                mapTransactionReceipt(receipt)
//            } else emptyList()
//        }
    }

    private fun rebuildRawTransaction(tx: Transaction): Transaction {
        val v = Numeric.toBytesPadded(BigInteger.valueOf(if (tx.v < 27) (tx.v + 27) else tx.v), 32)
        val r = Numeric.toBytesPadded(Numeric.toBigInt(tx.r), 32)
        val s = Numeric.toBytesPadded(Numeric.toBigInt(tx.s), 32)

        val rawTx = if (tx.maxPriorityFeePerGasRaw == null || tx.maxFeePerGasRaw == null) // pre EIP-1559
            SignedRawTransaction.createTransaction(
                tx.nonce,
                tx.gasPrice,
                tx.gas,
                tx.to,
                tx.value,
                tx.input
            )
        else SignedRawTransaction.createTransaction(  // post EIP-1559
            tx.chainId,
            tx.nonce,
            tx.gas,
            tx.to,
            tx.value,
            tx.input,
            tx.maxPriorityFeePerGas,
            tx.maxFeePerGas
        )

        val encodedTransaction = TransactionEncoder.encode(rawTx, Sign.SignatureData(v, r, s))

        tx.raw = Numeric.toHexString(encodedTransaction)

        require(tx.hash == Hash.sha3(tx.raw)) { "Invalid transaction format" }

        return tx
    }
}
