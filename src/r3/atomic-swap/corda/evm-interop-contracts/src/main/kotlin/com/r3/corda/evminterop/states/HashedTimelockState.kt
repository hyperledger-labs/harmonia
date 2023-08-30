package com.r3.corda.evminterop.states

import com.r3.corda.evminterop.contracts.HashedTimelockContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.CompositeKey
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.security.PublicKey

/**
 * HashedTimelockState class that is used as the lock state for Hashed-Timelock state of wrapped token transactions
 * that needs to be paid for.
 *
 * @property hash used to lock the wrapped-token transaction and prevent spend fro the receiver
 * @property timeWindow in which the wrapped-token transaction can be unlocked
 * @property creator party that issued the transaction and lock-state
 * @property receiver party for the wrapped-tokens transaction
 * @property paymentReceiverAddress the EOA expecting an ERC20 payment for the wrapped-token transaction
 */
@BelongsToContract(HashedTimelockContract::class)
data class HashedTimelockState(
    val hash: ByteArray,
    val timeWindow: TimeWindow,
    val creator: Party,
    val receiver: Party,
    val paymentReceiverAddress: String,
    override val participants: List<AbstractParty> = listOf(creator, receiver)
) : ContractState {

    val compositeKey: PublicKey = CompositeKey.Builder().addKeys(participants.map { it.owningKey }).build(1)

    // IMPORTANT: Property with 'Array' type in a 'data' class: it is recommended to override 'equals()' and 'hashCode()'

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HashedTimelockState

        if (!hash.contentEquals(other.hash)) return false
        if (timeWindow != other.timeWindow) return false
        if (creator != other.creator) return false
        if (receiver != other.receiver) return false
        if (paymentReceiverAddress != other.paymentReceiverAddress) return false
        if (participants != other.participants) return false
        if (compositeKey != other.compositeKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hash.contentHashCode()
        result = 31 * result + timeWindow.hashCode()
        result = 31 * result + creator.hashCode()
        result = 31 * result + receiver.hashCode()
        result = 31 * result + paymentReceiverAddress.hashCode()
        result = 31 * result + participants.hashCode()
        result = 31 * result + compositeKey.hashCode()
        return result
    }
}
