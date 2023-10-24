package com.r3.corda.evminterop.services

import com.r3.corda.evminterop.services.internal.RemoteEVMIdentityImpl
import org.web3j.crypto.RawTransaction
import java.net.URI

data class BridgeIdentityWithSigner(
    val signer: AuthorizedSigner,
    override val rpcEndpoint: URI,
    override val chainId: Long = -1,
    override val protocolAddress: String,
    override val deployerAddress: String
) : RemoteEVMIdentityImpl(rpcEndpoint, chainId, protocolAddress, deployerAddress) {

    override fun dispose() {
        signer.dispose()
    }

    override fun signMessage(rawTransaction: RawTransaction, chainId: Long) : ByteArray {
        throw NotImplementedError("Signer protocol not supported")
    }

    override fun getAddress(): String {
        throw NotImplementedError("Signer protocol not supported")
    }

    override fun signData(data: ByteArray) : ByteArray {
        throw NotImplementedError("Signer protocol not supported")
    }
}
