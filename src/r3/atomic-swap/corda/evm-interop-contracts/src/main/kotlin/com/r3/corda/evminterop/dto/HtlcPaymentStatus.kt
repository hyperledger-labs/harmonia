package com.r3.corda.evminterop.dto

import net.corda.core.serialization.CordaSerializable

/**
 * The 4 payment states a wrapped-token transaction can be in:
 *
 * PENDING - a locked wrapped-token tx has been issued and ownership transferred to a recipient that will pay for it
 * LOCKED - the recipient for the locked wrapped-token tx has deposited payment
 * WITHDRAWN - the issuer of the locked wrapped-token tx has withdrawn the payment releasing the hash pre-image (secret)
 * REVERTED - the issuer of the locked wrapped-token tx has reverted the wrapped-token tx
 * REMOVED - the pay and unlock fully succeeded on both sides, or something failed and both sides reverted.
 *
 */
@CordaSerializable
enum class HtlcPaymentStatus {
    PENDING,
    LOCKED,
    WITHDRAWN,
    REVERTED,
    REMOVED
}