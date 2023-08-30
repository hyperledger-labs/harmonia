package com.r3.corda.evminterop.services

import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowExternalOperation
import net.corda.core.utilities.loggerFor
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 *
 */
class ResponseOperation<T>(private val future: CompletableFuture<T>) :
    FlowExternalOperation<T> where  T : Any {
    companion object{
        val log = loggerFor<ResponseOperation<*>>()
    }

    private val operationId = UUID.randomUUID()

    init {
        log.info("Op id $operationId created (thread id ${Thread.currentThread().id })")
    }

    override fun execute(deduplicationId: String): T {
        log.info("Op id $operationId executing (thread id ${Thread.currentThread().id })")
        return try {
            val result = future.get()
            log.info("Op id $operationId executed (thread id ${Thread.currentThread().id })")
            result
        } catch (e: Exception) {
            log.error("Op id $operationId error (thread id ${Thread.currentThread().id }): $e ")
            throw FlowException("External API call failed", e)
        } finally {
            log.info("Op id $operationId exited (thread id ${Thread.currentThread().id })")
        }
    }
}