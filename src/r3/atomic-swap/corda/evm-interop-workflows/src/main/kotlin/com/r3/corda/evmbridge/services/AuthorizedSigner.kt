package com.r3.corda.evminterop.services

import net.corda.core.contracts.TimeWindow
import java.security.PublicKey
import java.time.Instant
import java.util.*

/**
 * Helper class to implement a remote signing feature probably using WalletConnect protocol
 */
class AuthorizedSigner private constructor(

    /**
     * [owningKey] of the authorized Corda Party or Account
     */
    val owningKey: PublicKey,

    /**
     * [account] of the authorized signer on the authorizing platform
     */
    val account: String,

    /**
     * [timeout] of the validity of this authorization
     */
    val timeout: TimeWindow = TimeWindow.between(Instant.MIN, Instant.MAX)

) {

    companion object {
        /**
         * Connects and request authorization according to the parameters
         * @param [owningKey] of the authorized Party
         * @param [account] of the authorized signer
         * @param [timeout] of the validity of this authorization
         * @param [flowId] of the authorized flow
         */
        fun requestAuthorization(
            owningKey: PublicKey,
            account: String,
            timeout: TimeWindow = TimeWindow.between(Instant.MIN, Instant.MAX)
        ) : AuthorizedSigner {
            return AuthorizedSigner(owningKey, account, timeout)
        }
    }

    /**
     * Invalidates the current signing authorization, so that no other flow can use it
     */
    fun dispose() {
    }
}
