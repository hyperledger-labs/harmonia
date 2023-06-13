package com.r3.corda.evmbridge.services

import com.r3.corda.cno.evmbridge.dto.Log
import com.r3.corda.cno.evmbridge.dto.TransactionReceipt
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture

/**
 * Provides a store for Ethereum call/transaction non-blocking requests. The data object provides a key (as
 * transactionHash) and the completable future that is passed to the outer/service caller. When the polling
 * module receives confirmation, or error, or timeout about the given transaction hash, it completes the
 * future response returning the receipt, or error.
 */
data class CompletableTransaction(
    val transactionHash: String,
    private val future: CompletableFuture<TransactionReceipt>,
    // NOTE: review timeout to be based on network configuration
    private val timeout: Duration = Duration.ofMinutes(5)
) {
    @Volatile
    var inFlight: Boolean = false

    @Volatile
    var isComplete: Boolean = false

    private val expiresAt: Instant = Instant.now().plusSeconds(timeout.seconds)

    init {
        require(timeout > Duration.ZERO) { "Timeout cannot be negative" }
    }

    val isExpired: Boolean
        get() {
            return Instant.now().isAfter(expiresAt)
        }

    fun complete(transactionReceipt: TransactionReceipt) {
        setForRemove()
        future.complete(transactionReceipt)
    }

    fun completeWithTimeout() {
        setForRemove()
        future.completeExceptionally(
            Throwable("Timeout while waiting for transaction receipt (tx $transactionHash)")
        )
    }

    fun completeError(message: String) {
        setForRemove()
        future.completeExceptionally(
            Throwable("Error while waiting for transaction receipt (tx $transactionHash): $message")
        )
    }

    private fun setForRemove() {
        inFlight = false
        isComplete = true
    }
}

data class CompletableEvent(
        private val future: CompletableFuture<Log>,
        // NOTE: review timeout to be based on network configuration
        private val timeout: Duration = Duration.ofMinutes(5)
) {
    @Volatile
    var inFlight: Boolean = false

    @Volatile
    var isComplete: Boolean = false

    private val expiresAt: Instant = Instant.now().plusSeconds(timeout.seconds)

    init {
        require(timeout > Duration.ZERO) { "Timeout cannot be negative" }
    }

    val isExpired: Boolean
        get() {
            return Instant.now().isAfter(expiresAt)
        }

    fun complete(eventLog: Log) {
        setForRemove()
        future.complete(eventLog)
    }

//    fun completeWithTimeout() {
//        setForRemove()
//        future.completeExceptionally(
//                Throwable("Timeout while waiting for transaction receipt (tx $transactionHash)")
//        )
//    }
//
//    fun completeError(message: String) {
//        setForRemove()
//        future.completeExceptionally(
//                Throwable("Error while waiting for transaction receipt (tx $transactionHash): $message")
//        )
//    }

    private fun setForRemove() {
        inFlight = false
        isComplete = true
    }
}
