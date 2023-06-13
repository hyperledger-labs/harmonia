package com.r3.corda.cno.evmbridge.dto

import net.corda.core.serialization.CordaSerializable
import java.math.BigInteger

/**
 * The 6 states a payment can be in are the following:
 *
 * WAITING - When no deposit done (or no such deposit is expected!) and timeout not expired.
 * TIMEOUT - Same conditions as above, but timeout expired.
 * REVERTED - When a payment was locked-in earlier but met the revert conditions and was reverted (does not time-out).
 * WITHDRAWN - When a payment was locked-in earlier and was withdrawn by the expected recipient (does not time-out).
 *             In this case, the pre-image of the lock-hash is returned with the status response.
 * READY - When a payment is locked-in and ready to be withdrawn (timeout did not expire yet).
 * EXPIRED - When a payment is locked-in but it timed-out and therefore can only be reverted.
 */
@CordaSerializable
enum class DepositStates(val value: BigInteger) {
    WAITING(0.toBigInteger()),
    READY(1.toBigInteger()),
    WITHDRAWN(2.toBigInteger()),
    EXPIRED(3.toBigInteger()),
    REVERTED(4.toBigInteger()),
    TIMEOUT(5.toBigInteger());
    companion object {
        fun fromBigInteger(value: BigInteger) = DepositStates.values().first { it.value == value }
    }
}