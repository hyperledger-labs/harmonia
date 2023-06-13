package com.r3.corda.evmbridge.test

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.crypto.CompositeKey
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@BelongsToContract(GenericAssetContract::class)
class GenericAssetState (override val owner: AbstractParty) : OwnableState {
    override val participants: List<AbstractParty>
        get() = if (owner.owningKey is CompositeKey) {
            (owner.owningKey as CompositeKey).children.map { AnonymousParty(it.node) }
        } else {
            listOf(owner)
        }

    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        return CommandAndState(GenericAssetContract.GenericAssetCommand.Sell, GenericAssetState(newOwner))
    }
}

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

@StartableByRPC
class IssueGenericAssetFlow : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val asset = GenericAssetState(ourIdentity)
        val command = Command(GenericAssetContract.GenericAssetCommand.Issue, listOf(ourIdentity.owningKey))
        val txBuilder = TransactionBuilder(notary = serviceHub.networkMapCache.notaryIdentities.first())
            .addOutputState(asset)
            .addCommand(command)
        val ptx = serviceHub.signInitialTransaction(txBuilder)
        val stx = subFlow(CollectSignaturesFlow(ptx, emptyList()))
        return subFlow(FinalityFlow(stx, emptyList()))
    }
}