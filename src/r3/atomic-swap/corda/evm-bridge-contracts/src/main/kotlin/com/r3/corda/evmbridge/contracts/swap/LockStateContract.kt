package com.r3.corda.evmbridge.contracts.swap

import com.r3.corda.evmbridge.states.swap.LockState
import com.r3.corda.evmbridge.states.swap.TransferProof
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.DigitalSignature
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

class LockStateContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        val cmd = tx.commandsOfType<LockCommand>().singleOrNull()?.value
        require (cmd != null) { "Only one LockCommand can be used" }
        "Only one asset state can be locked or reverted/unlocked" using (tx.outputStates.filterIsInstance<OwnableState>().size == 1 && tx.inputStates.filterIsInstance<OwnableState>().size == 1)

        when (cmd) {
            is LockCommand.Lock -> {
                requireThat {
                    "Transaction cannot have any input states of type net.corda.swap.contracts.LockState" using (tx.inputStates.filterIsInstance<LockState>().isEmpty())
                    "Only one net.corda.swap.contracts.LockState can be generated" using (tx.outputStates.filterIsInstance<LockState>().size == 1)
                    "Exactly one asset state can be generated alongside a net.corda.swap.contracts.LockState" using (tx.outputStates.size  == 2)
                    val lockedAssetState = tx.outputStates.filterIsInstance<OwnableState>().single()
                    val lockState = tx.outputStates.filterIsInstance<LockState>().single()
                    "Asset state needs to be owned by the composite key created from original owner and new owner public keys" using((lockedAssetState.owner.owningKey as CompositeKey).isFulfilledBy(setOf(lockState.senderCordaAddress, lockState.recipientCordaAddress)))
                    "Required number of validator signatures is greater than the number of approved validators" using (lockState.minimumNumberOfValidatorSignatures <= lockState.approvedValidators.size)
                }
            }
            is LockCommand.Revert -> {
                val unlockedAssetState = tx.outputStates.filterIsInstance<OwnableState>().single()
                val lockState = tx.inputStates.filterIsInstance<LockState>().single()
                requireThat {
                    "Only two input states can exist" using (tx.inputStates.size == 2)
                    "Asset can only be reverted to original owner" using (unlockedAssetState.owner.owningKey == lockState.senderCordaAddress)
                    // TODO: verify proof which allows reversal of Corda asset to original owner
                }
            }
            is LockCommand.Unlock -> {
                val unlockedAssetState = tx.outputStates.filterIsInstance<OwnableState>().single()
                val lockState = tx.inputStates.filterIsInstance<LockState>().single()
                val lockedAssetTxId = tx.inRefsOfType<OwnableState>().single().ref.txhash
                requireThat {
                    "Only two input states can exist" using (tx.inputStates.size == 2)
                    "Asset can only be transferred to new owner" using (unlockedAssetState.owner.owningKey == lockState.recipientCordaAddress)
                    "EVM Transfer event references the wrong Corda transaction" using (lockedAssetTxId == cmd.proof.transferEvent.txId)
                    "EVM Transfer event has not been validated by the minimum number of validators" using (cmd.proof.validatorSignatures.size >= lockState.minimumNumberOfValidatorSignatures)
                    "One or more validator signatures cannot be verified" using (verifyValidatorSignatures(cmd.proof.validatorSignatures, lockedAssetTxId, lockState.approvedValidators))
                }
            }
        }
    }

    private fun verifyValidatorSignatures(sigs: List<DigitalSignature.WithKey>, txId: SecureHash, approvedValidators: List<PublicKey>): Boolean {
        sigs.forEach {
            val validator = it.by
            if (!approvedValidators.contains(validator) || !it.verify(txId.bytes))
                return false

        }

        return true
    }
}

sealed class LockCommand : CommandData {
    object Lock : LockCommand()
    class Revert(val proof: TransferProof) : LockCommand()
    class Unlock(val proof: TransferProof) : LockCommand()
}