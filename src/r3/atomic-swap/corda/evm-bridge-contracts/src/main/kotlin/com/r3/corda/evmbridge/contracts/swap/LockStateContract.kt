package com.r3.corda.evmbridge.contracts.swap

import com.r3.corda.cno.evmbridge.dto.encoded
import com.r3.corda.evmbridge.states.swap.LockState
import com.r3.corda.evmbridge.states.swap.UnlockData
import com.r3.corda.interop.evm.common.trie.PatriciaTrie
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.DigitalSignature
import net.corda.core.transactions.LedgerTransaction
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpString
import org.web3j.utils.Numeric
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
                    "Asset state needs to be owned by the composite key created from original owner and new owner public keys" using((lockedAssetState.owner.owningKey as CompositeKey).isFulfilledBy(setOf(lockState.assetSender, lockState.assetRecipient)))
                    "Required number of validator signatures is greater than the number of approved validators" using (lockState.signaturesThreshold <= lockState.approvedValidators.size)
                    // REVIEW: must check forward/backward events
                }
            }
            is LockCommand.Revert -> {
                val unlockedAssetState = tx.outputStates.filterIsInstance<OwnableState>().single()
                val lockState = tx.inputStates.filterIsInstance<LockState>().single()
                val txIndexKey = RlpEncoder.encode(RlpString.create(Numeric.toBigInt(cmd.proof.transactionReceipt.transactionIndex!!).toLong()))
                val receiptsRoot = Numeric.hexStringToByteArray(cmd.proof.receiptsRootHash)
                val leafData = cmd.proof.transactionReceipt.encoded()

                requireThat {
                    "Only two input states can exist" using (tx.inputStates.size == 2)
                    "Invalid recipient for this command" using (unlockedAssetState.owner.owningKey == lockState.assetSender)
                    "The transaction receipt does not contain the expected unlock event" using(lockState.backwardEvent.isFoundIn(cmd.proof.transactionReceipt))
                    "The transaction receipts merkle proof failed to validate" using(PatriciaTrie.verifyMerkleProof(receiptsRoot, txIndexKey, leafData, cmd.proof.merkleProof))
                    "One or more validator signatures failed to verify block inclusion" using (verifyValidatorSignatures(cmd.proof.validatorSignatures, receiptsRoot, lockState.approvedValidators))
                }
            }
            is LockCommand.Unlock -> {
                val unlockedAssetState = tx.outputStates.filterIsInstance<OwnableState>().single()
                val lockState = tx.inputStates.filterIsInstance<LockState>().single()
                val txIndexKey = RlpEncoder.encode(RlpString.create(Numeric.toBigInt(cmd.proof.transactionReceipt.transactionIndex!!).toLong()))
                val receiptsRoot = Numeric.hexStringToByteArray(cmd.proof.receiptsRootHash)
                val leafData = cmd.proof.transactionReceipt.encoded()

                requireThat {
                    "Only two input states can exist" using (tx.inputStates.size == 2)
                    "Invalid recipient for this command" using (unlockedAssetState.owner.owningKey == lockState.assetRecipient)
                    "EVM Transfer event has not been validated by the minimum number of validators" using (cmd.proof.validatorSignatures.size >= lockState.signaturesThreshold)
                    "The transaction receipt does not contain the expected unlock event" using(lockState.forwardEvent.isFoundIn(cmd.proof.transactionReceipt))
                    "The transaction receipts merkle proof failed to validate" using(PatriciaTrie.verifyMerkleProof(receiptsRoot, txIndexKey, leafData, cmd.proof.merkleProof))
                    "One or more validator signatures failed to verify block inclusion" using (verifyValidatorSignatures(cmd.proof.validatorSignatures, receiptsRoot, lockState.approvedValidators))
                }
            }
        }
    }

    private fun verifyValidatorSignatures(sigs: List<DigitalSignature.WithKey>, signableData: ByteArray, approvedValidators: List<PublicKey>): Boolean {
        sigs.forEach {
            val validator = it.by
            if (!approvedValidators.contains(validator) || !it.verify(signableData))
                return false
        }

        return true
    }
}

sealed class LockCommand : CommandData {
    object Lock : LockCommand()
    class Revert(val proof: UnlockData) : LockCommand()
    class Unlock(val proof: UnlockData) : LockCommand()
}