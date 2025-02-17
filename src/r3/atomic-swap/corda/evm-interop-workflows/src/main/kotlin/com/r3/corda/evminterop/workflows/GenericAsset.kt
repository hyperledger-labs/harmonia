package com.r3.corda.evminterop.workflows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.crypto.CompositeKey
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.TransactionBuilder
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * Definition of a Generic Asset Schema
 */
object GenericAssetSchema

object GenericAssetSchemaV1 : MappedSchema(
    schemaFamily = GenericAssetSchema.javaClass,
    version = 1,
    mappedTypes = listOf(PersistentGenericAsset::class.java)
) {
    override val migrationResource: String?
        get() = "iou.changelog-master";


    @Entity
    @Table(name = "generic_asset_states")
    class PersistentGenericAsset(
        @Column(name = "asset_name")
        var assetName: String = ""
    ) : PersistentState()
}

/**
 * Definition of a Generic Asset State
 */
@BelongsToContract(GenericAssetContract::class)
class GenericAssetState (val assetName: String, override val owner: AbstractParty) : OwnableState, QueryableState {
    override val participants: List<AbstractParty>
        get() = if (owner.owningKey is CompositeKey) {
            (owner.owningKey as CompositeKey).children.map { AnonymousParty(it.node) }
        } else {
            listOf(owner)
        }

    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        return CommandAndState(GenericAssetContract.GenericAssetCommand.Sell, GenericAssetState(assetName, newOwner))
    }

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is GenericAssetSchemaV1 -> GenericAssetSchemaV1.PersistentGenericAsset(
                assetName = this.assetName
            )
            else -> throw IllegalArgumentException("Unrecognized schema: $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(GenericAssetSchemaV1)
}

/**
 * Definition of a Generic Asset Contract
 */
class GenericAssetContract : Contract {

    companion object {
        val ID: ContractClassName get() = this::class.java.enclosingClass.canonicalName
    }

    override fun verify(tx: LedgerTransaction) {

    }

    sealed class GenericAssetCommand : CommandData {
        object Issue : GenericAssetCommand()
        object Sell : GenericAssetCommand()
        object Consume : GenericAssetCommand()
    }
}

/**
 * Helper flow to issue a Generic Asse
 */
@StartableByRPC
@InitiatingFlow
class IssueGenericAssetFlow(private val assetName: String) : FlowLogic<StateRef>() {
    @Suspendable
    override fun call(): StateRef {
        val asset = GenericAssetState(assetName, ourIdentity)
        val command = Command(GenericAssetContract.GenericAssetCommand.Issue, listOf(ourIdentity.owningKey))
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
            .addOutputState(asset)
            .addCommand(command)

        val outputIndex = txBuilder.outputStates().size - 1

        val ptx = serviceHub.signInitialTransaction(txBuilder)
        val stx = subFlow(CollectSignaturesFlow(ptx, emptyList()))
        val notarizedTx = subFlow(FinalityFlow(stx, emptyList()))
        return StateRef(notarizedTx.id, outputIndex)
    }
}
/**
 * Helper flow to query a Generic Asset from the Vault
 */
@StartableByRPC
@InitiatingFlow
class QueryGenericAssetFlow(private val assetName: String) : FlowLogic<List<StateAndRef<GenericAssetState>>>() {

    constructor() : this("")

    @Suspendable
    override fun call(): List<StateAndRef<GenericAssetState>> {

        return if(assetName.isEmpty()) {
            serviceHub.vaultService.queryBy(GenericAssetState::class.java).states
        } else {
            serviceHub.vaultService.queryBy(GenericAssetState::class.java, queryCriteria(assetName)).states
        }
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
}
