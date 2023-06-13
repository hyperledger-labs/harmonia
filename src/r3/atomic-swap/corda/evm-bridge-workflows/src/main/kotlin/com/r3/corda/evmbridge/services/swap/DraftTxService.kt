package com.r3.corda.evmbridge.services.swap

import net.corda.core.crypto.SecureHash
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple [CordaService] used to store and retrieve swap transaction information
 * TODO: Current implementation is suitable only for testing. A more robust approach is needed
 */
@CordaService
class DraftTxService(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    private val transactions = ConcurrentHashMap<SecureHash, WireTransaction>()

    fun saveDraftTx(tx: WireTransaction) {
        if (transactions.containsKey(tx.id))
            throw IllegalStateException("Transaction with ID ${tx.id} already exists in storage")
        transactions[tx.id] = tx
    }

    fun getDraftTx(id: SecureHash): WireTransaction? {
        return transactions.getOrDefault(id, null)
    }

    fun deleteDraftTx(id: SecureHash) {
        transactions.remove(id)
    }

    fun getDraftTxDependencies(id: SecureHash): List<SignedTransaction> {
        val wireTx = getDraftTx(id) ?: return emptyList()
        val wireTxDependenciesHashes = wireTx.inputs.map { it.txhash }.toSet() + wireTx.references.map { it.txhash }.toSet()
        return  wireTxDependenciesHashes.mapNotNull {
            serviceHub.validatedTransactions.getTransaction(it)
        }.toList()
    }
}