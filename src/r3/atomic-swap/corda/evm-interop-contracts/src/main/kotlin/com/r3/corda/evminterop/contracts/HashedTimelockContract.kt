package com.r3.corda.evminterop.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.transactions.LedgerTransaction

/**
 * HashedTimelockContract is the Hashed-Timelock contract used as an encumbrance on the wrapped-token transaction
 * that allows Lock, Release and Revert of the wrapped-tokens.
 */
class HashedTimelockContract : Contract {
    companion object {
        val ID: ContractClassName get() = this::class.java.enclosingClass.canonicalName
    }
    override fun verify(tx: LedgerTransaction) {
    }

    open class LockCommand : CommandData
    class Lock : LockCommand()
    class Release(val secret: String) : LockCommand()
    class Revert : LockCommand()

}
