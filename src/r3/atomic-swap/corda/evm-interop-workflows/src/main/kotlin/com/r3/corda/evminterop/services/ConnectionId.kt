package com.r3.corda.evminterop.services

import java.net.URI

/**
 * [ConnectionId] is the key correlation between a [Session] and its [Connection]
 */
internal data class ConnectionId(
    internal val rpcEndpoint: URI,
    internal val chainId: Long
) {
    companion object {
        private val httpRegex = Regex("^https?$", RegexOption.IGNORE_CASE)
        private val wsRegex = Regex("^wss?$", RegexOption.IGNORE_CASE)
    }

    internal val isHttp: Boolean = httpRegex.matches(rpcEndpoint.scheme)
    internal val isWebsocket: Boolean = wsRegex.matches(rpcEndpoint.scheme)

    init {
        require(isHttp || isWebsocket) {"Invalid rpcEndpoint URI: $rpcEndpoint"}
    }

    override fun toString(): String {
        return "ConnectionId(rpcEndpoint=$rpcEndpoint, chainId=$chainId, isHttp=$isHttp, isWebsocket=$isWebsocket)"
    }
}
