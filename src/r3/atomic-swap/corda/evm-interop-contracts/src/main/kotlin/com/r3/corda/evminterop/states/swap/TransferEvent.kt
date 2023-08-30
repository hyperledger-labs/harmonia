package com.r3.corda.evminterop.states.swap

import net.corda.core.crypto.SecureHash
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class TransferEvent(val txId: SecureHash,
                         val detailsHash: SecureHash)