package com.r3.corda.evmbridge.states.swap

import com.r3.corda.evmbridge.EncodedEvent
import com.r3.corda.evmbridge.contracts.swap.LockStateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import java.security.PublicKey

@BelongsToContract(LockStateContract::class)
class LockState(val senderCordaAddress: PublicKey,
                val recipientCordaAddress: PublicKey,
                val approvedValidators: List<PublicKey>,
                val minimumNumberOfValidatorSignatures: Int,
                val evmBlockchainId: Int,
                val forwardEvent: EncodedEvent,
                val backwardEvent: EncodedEvent,
                override val participants: List<AbstractParty> = emptyList()) : ContractState