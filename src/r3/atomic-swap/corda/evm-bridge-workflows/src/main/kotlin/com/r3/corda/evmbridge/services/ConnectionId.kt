package com.r3.corda.evmbridge.services

import java.net.URI

/**
 * [ConnectionId] is the key correlation between a [Session] and its [Connection]
 */
data class ConnectionId(
    val rpcEndpoint: URI,
    val chainId: Long
) {
    companion object {
        val httpRegex = Regex("^https?$", RegexOption.IGNORE_CASE)
        val wsRegex = Regex("^wss?$", RegexOption.IGNORE_CASE)
    }

    val isHttp: Boolean = httpRegex.matches(rpcEndpoint.scheme)
    val isWebsocket: Boolean = wsRegex.matches(rpcEndpoint.scheme)

    init {
        require(isHttp || isWebsocket) {"Invalid rpcEndpoint URI: $rpcEndpoint"}
    }

    override fun toString(): String {
        return "ConnectionId(rpcEndpoint=$rpcEndpoint, chainId=$chainId, isHttp=$isHttp, isWebsocket=$isWebsocket)"
    }
}