package com.r3.corda.evminterop.dto

import net.corda.core.serialization.CordaSerializable
import java.time.Instant

/**
 * Represents the status of payment for a locked wrapped-token transaction payment.
 *
 * The 6 states a payment can be in are the following:
 * WAITING - When no deposit done (or no such deposit is expected!) and timeout not expired.
 * TIMEOUT - Same conditions as above, but timeout expired.
 * REVERTED - When a payment was locked-in earlier but met the revert conditions and was reverted (does not time-out).
 * WITHDRAWN - When a payment was locked-in earlier and was withdrawn by the expected recipient (does not time-out).
 *             In this case, the pre-image of the lock-hash is returned with the status response.
 * READY - When a payment is locked-in and ready to be withdrawn (timeout did not expire yet).
 * EXPIRED - When a payment is locked-in but it timed-out and therefore can only be reverted.
 *
 * @property status as described above.
 * @property canExecute true if the caller can execute an action (e.g. withdraw during READY, or revert during EXPIRED).
 * @property secret contains the pre-image of the hash-lock if the payment was unlocked and withdrawn.
 * @property lockUntil the timeout before which the payment needs to complete.
 * @property timestamp the evm blockchain timestamp at the time of the response
 */
@CordaSerializable
data class DepositStatus(
    val status: DepositStates,
    val canExecute: Boolean,
    val secret: String,
    val lockUntil: Instant,
    val timestamp: Instant
) {
    companion object {
        fun NotFound() = DepositStatus(DepositStates.WAITING, false, "", Instant.MIN, Instant.MIN)
    }

    override fun toString(): String {
        return "DepositStatus(status=$status, canExecute=$canExecute, secret='$secret', lockUntil=$lockUntil, timestamp=$timestamp)"
    }
}