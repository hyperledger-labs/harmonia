package com.r3.corda.evmbridge.states.swap

import com.r3.corda.evmbridge.EncodedEvent
import net.corda.core.contracts.OwnableState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

/**
 * Simple data structure describing the atomic swap details agreed upon by the involved parties.
 * Contains information to identify the sender, receiver, and assets being exchanged on both Corda and EVM networks as
 * well as a pool of approved validators/oracles (Corda nodes) which can verify and attest EVM events, and the minimum
 * number of validations required for the atomics swap protocol to succeed.
 */
@CordaSerializable
data class SwapTransactionDetails(val senderCordaName: Party,
                                  val receiverCordaName: Party,
                                  val cordaAssetState: StateAndRef<OwnableState>,
                                  val evmAssetContractAddress: String,
                                  val approvedCordaValidators: List<Party>,
                                  val minimumNumberOfEventValidations: Int,
                                  val evmBlockchainId: Int,
                                  val forwardEvent: EncodedEvent,
                                  val backwardEvent: EncodedEvent)