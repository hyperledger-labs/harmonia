package com.r3.corda.evminterop.states.swap

import com.r3.corda.evminterop.EncodedEvent
import com.r3.corda.evminterop.contracts.swap.LockStateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import java.security.PublicKey

/**
 * The [LockState] data structure allows to define the Unlock/Revert expectations
 */
@BelongsToContract(LockStateContract::class)
class LockState(val assetSender: PublicKey,
                val assetRecipient: PublicKey,
                val notary: PublicKey,
                val approvedValidators: List<PublicKey>,
                val signaturesThreshold: Int,
                val forwardEvent: EncodedEvent,
                val backwardEvent: EncodedEvent,
                override val participants: List<AbstractParty> = emptyList()) : ContractState
