package com.r3.corda.evmbridge.states.swap

import com.r3.corda.evmbridge.EncodedEvent
import com.r3.corda.evmbridge.contracts.swap.LockStateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import java.security.PublicKey

@BelongsToContract(LockStateContract::class)
class LockState(val assetSender: PublicKey,
                val assetRecipient: PublicKey,
                val approvedValidators: List<PublicKey>,
                val signaturesThreshold: Int,
                val forwardEvent: EncodedEvent,
                val backwardEvent: EncodedEvent,
                override val participants: List<AbstractParty> = emptyList()) : ContractState

// REVIEW: add notary
