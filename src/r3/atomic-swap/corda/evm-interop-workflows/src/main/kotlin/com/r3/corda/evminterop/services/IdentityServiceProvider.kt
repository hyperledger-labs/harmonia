package com.r3.corda.evminterop.services

import com.r3.corda.evminterop.*
import com.r3.corda.evminterop.dto.*
import com.r3.corda.evminterop.dto.TransactionReceipt
import net.corda.core.flows.FlowExternalOperation
import net.corda.core.flows.FlowLogic
import net.corda.core.node.AppServiceHub
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.RawTransaction
import org.web3j.generated.contracts.*
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.RemoteFunctionCall
import org.web3j.protocol.core.methods.response.*
import org.web3j.service.TxSignService
import org.web3j.tuples.generated.Tuple2
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.security.InvalidParameterException
import java.security.PublicKey
import java.util.concurrent.CompletableFuture
import kotlin.streams.toList

/**
 * Corda service implementing access to external EVM network functions.
 */
@CordaService
class IdentityServiceProvider(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    private companion object {
        val log = loggerFor<IdentityServiceProvider>()

        private val remoteConnections : MutableMap<ConnectionId, Connection> = mutableMapOf()
        private val remoteIdentities : MutableMap<PublicKey, Session> = mutableMapOf()

        /**
         * Creates or load an instance of the EVM network connection identified by the unique [connectionId]
         */
        private fun connection(connectionId: ConnectionId) : Connection {
            return remoteConnections.computeIfAbsent(connectionId) { Connection(it) }
        }

        /**
         * Load and initializes an instance of the RawTransactionManager used by the RemoteEVMIdentity instance
         * to sign raw transaction messages.
         */
        private fun loadTransactionManager(connection: Connection, txSignService: TxSignService) : RawTransactionManager {
            return RawTransactionManager(connection.web3j, txSignService, connection.chainId)
        }

        /**
         * Load and initializes the contract instance for the ERC20 token
         */
        private fun loadToken(tokenAddress: String, connectionId: ConnectionId, txSignService: TxSignService) : ERC20 {
            val rawTransactionManager = loadTransactionManager(connection(connectionId), txSignService)
            return ERC20.load(tokenAddress, connection(connectionId).web3j, rawTransactionManager, DefaultGasProvider())
        }

        /**
         * Load and initializes the contract instance for the ERC20 token
         */
        private fun loadSwapVault(contractAddress: String, connectionId: ConnectionId, txSignService: TxSignService) : SwapVault {
            val rawTransactionManager = loadTransactionManager(connection(connectionId), txSignService)
            return SwapVault.load(contractAddress, connection(connectionId).web3j, rawTransactionManager, DefaultGasProvider())
        }

        /**
         * Load and initializes the contract instance for the Bridge Protocol
         */
        private fun loadBridge(bridgeAddress: String, connectionId: ConnectionId, txSignService: TxSignService) : HashedTimelockBridge {
            val rawTransactionManager = loadTransactionManager(connection(connectionId), txSignService)
            return HashedTimelockBridge.load(bridgeAddress, connection(connectionId).web3j, rawTransactionManager, DefaultGasProvider())
        }

        /**
         * Load and initializes the contract instance for the Contracts Protocol
         */
        private fun loadContracts(contractsAddress: String, connectionId: ConnectionId, txSignService: TxSignService) : BridgeDeploy {
            val rawTransactionManager = loadTransactionManager(connection(connectionId), txSignService)
            return BridgeDeploy.load(contractsAddress, connection(connectionId).web3j, rawTransactionManager, DefaultGasProvider())
        }

        private val addressRegEx = Regex("^0x[0-9a-fA-F]{40}\$")
        fun isValidAddress(address: String) = addressRegEx.matches(address)
    }

    /**
     * Initializes a RemoteEVMIdentity instance
     */
    fun authorize(remoteIdentity: RemoteEVMIdentity, authorizedId: PublicKey) {
        require(!remoteIdentities.containsKey(authorizedId)) {
            "Authorization for identity $authorizedId was already requested"
        }

        remoteIdentities[authorizedId] = Session(
            remoteIdentity,
            ConnectionId(
                remoteIdentity.rpcEndpoint,
                remoteIdentity.chainId
            )
        )
    }

    /**
     * Load the session for the authorized identity if any exists, or throw.
     */
    private fun session(authorizedId: PublicKey): Session {
        return remoteIdentities[authorizedId] ?: throw InvalidParameterException("No session found with id $authorizedId")
    }

    /**
     * Load the [ERC20Wrapper] for the given token and identity
     */
    fun erc20(tokenAddress: String, authorizedId: PublicKey) : IERC20 {
        return ERC20Wrapper(tokenAddress, session(authorizedId))
    }

    /**
     * Load the [SwapVaultWrapper] for the given token and identity
     */
    fun swap(authorizedId: PublicKey) : ISwapVault {
        return SwapVaultWrapper(session(authorizedId))
    }

    /**
     * Load the EVM contracts protocol for the given identity
     */
    fun contracts(authorizedId: PublicKey) : IContracts {
        return ContractsWrapper(session(authorizedId))
    }

    /**
     * Load an EVM interface for the given identity that implements generic EVM functions as per interface [IEther]
     */
    fun ethers(authorizedId: PublicKey) : IEther {
        return EtherImpl(session(authorizedId))
    }

    /**
     * Exposes an interface to access Web3 interfaces. Currently only implements functions available on test network
     * and therefore should only be visible to test projects.
     */
    fun web3(authorizedId: PublicKey): IWeb3 {
        return Web3Impl(session(authorizedId))
    }

    /**
     * The public key / address for the given identity / signer.
     */
    fun signerAddress(authorizedId: PublicKey): String {
        return session(authorizedId).address
    }

    /**
     * The address for EVM network deployed bridge protocol contract
     */
    fun protocolAddress(authorizedId: PublicKey): String {
        return session(authorizedId).protocolAddress
    }

    /**
     * A helper class that represents a session related to a connection.
     * One connection can serve multiple sessions.
     */
    private class Session(
        private val remoteIdentity: RemoteEVMIdentity,
        val connectionId: ConnectionId
    ) : TxSignService {

        /**
         * Bridge session -> connection helper function [simpleRemoteFunctionCall]
         */
        fun<T> simpleRemoteFunctionCall(fn: RemoteFunctionCall<T>): ResponseOperation<T> where  T : Any {
            return connection(connectionId).simpleRemoteFunctionCall(fn)
        }

        /**
         * Bridge session -> connection helper function [simpleRemoteFunctionCall]
         */
        fun<T,R> simpleRemoteFunctionCall(fn: RemoteFunctionCall<T>, transform: (T) -> R):
                ResponseOperation<R> where  T : Any, R: Any {
            return connection(connectionId).simpleRemoteFunctionCall(fn, transform)
        }

        /**
         * Bridge session -> connection helper function [queueRemoteFunctionCall]
         */
        fun queueRemoteFunctionCall(fn: RemoteFunctionCall<org.web3j.protocol.core.methods.response.TransactionReceipt>): ResponseOperation<TransactionReceipt> {
            return connection(connectionId).queueRemoteFunctionCall(fn)
        }

        fun queueRemoteEventLog(address: String): ResponseOperation<TransactionReceiptLog> {
            return connection(connectionId).queueEventLogResponse(address)
        }

        /**
         * Bridge session -> TxSignService function [sign] / [signMessage]
         */
        override fun sign(rawTransaction: RawTransaction?, chainId: Long): ByteArray {
            return remoteIdentity.signMessage(rawTransaction!!, chainId)
        }

        /**
         * Bridge session -> TxSignService function [getAddress]
         */
        override fun getAddress(): String {
            return remoteIdentity.getAddress()
        }

        /**
         * Bridge session -> RemoteEVMIdentity function [protocolAddress]
         */
        val protocolAddress = remoteIdentity.protocolAddress

        /**
         * Bridge session -> RemoveEVMIdentity function [deployerAddress]
         */
        val deployerAddress = remoteIdentity.deployerAddress

        /**
         * Bridge session -> connection function [ethCall]
         */
        fun <T> ethCall(method: String, params: List<T>): CompletableFuture<EthCall> {
            return connection(connectionId).ethCall(method, params)
        }

        /**
         * Bridge session -> connection function [getBalance]
         */
        fun getBalance(address: String, blockParameterName: DefaultBlockParameter): CompletableFuture<EthGetBalance> {
            return connection(connectionId).getBalance(address, blockParameterName)
        }

        fun getBlockByNumber(number: BigInteger, fullTransactionObjects: Boolean): CompletableFuture<Block> {
            return connection(connectionId).getBlockByNumber(number, fullTransactionObjects)
        }

        fun getBlockByHash(hash: String, fullTransactionObjects: Boolean): CompletableFuture<Block> {
            return connection(connectionId).getBlockByHash(hash, fullTransactionObjects)
        }

        fun getTransactionByHash(hash: String): CompletableFuture<com.r3.corda.evminterop.dto.Transaction> {
            return connection(connectionId).getTransactionByHash(hash)
        }

        fun getTransactionReceiptByHash(hash: String): CompletableFuture<TransactionReceipt> {
            return connection(connectionId).getTransactionReceiptByHash(hash)
        }

        fun getBlockReceipts(blockNumber: BigInteger): CompletableFuture<List<TransactionReceipt>> {
            return connection(connectionId).getBlockReceipts(blockNumber)
        }
    }

    /**
     * A wrapper to the [ERC20] - [IERC20] pair: exposes [IERC20] and maps [ERC20] calls to queueable
     * [FlowExternalOperation]s. Implements standard ERC20 interface:
     * https://ethereum.org/it/developers/docs/standards/tokens/erc-20/
     */
    private class ERC20Wrapper(
        private val tokenAddress: String,
        private val session: Session
    ) : IERC20 {

        private fun token() : ERC20 {
            return loadToken(tokenAddress, session.connectionId, session as TxSignService)
        }

        // ERC20 Extension

        override fun name(): FlowExternalOperation<String> {
            return session.simpleRemoteFunctionCall(token().name())
        }

        override fun symbol(): FlowExternalOperation<String> {
            return session.simpleRemoteFunctionCall(token().symbol())
        }

        override fun decimals(): FlowExternalOperation<BigInteger> {
            return session.simpleRemoteFunctionCall(token().decimals())
        }

        override fun totalSupply(): FlowExternalOperation<BigInteger> {
            return session.simpleRemoteFunctionCall(token().totalSupply())
        }

        // transactional functions

        override fun approve(receiverAddress: String, amount: BigInteger): FlowExternalOperation<TransactionReceipt> {
            require(isValidAddress(receiverAddress)) { "Invalid receiverAddress $receiverAddress" }
            require(amount >= BigInteger.ZERO) { "Amount must be positive" }

            return session.queueRemoteFunctionCall(token().approve(receiverAddress, amount))
        }

        override fun transfer(receiverAddress: String, amount: BigInteger): FlowExternalOperation<TransactionReceipt> {
            require(isValidAddress(receiverAddress)) { "Invalid receiverAddress $receiverAddress" }

            log.info("Transferring $amount of ERC20($tokenAddress) to $receiverAddress.")

            return session.queueRemoteFunctionCall(token().transfer(receiverAddress, amount))
        }

        override fun transferFrom(
            senderAddress: String,
            receiverAddress: String,
            amount: BigInteger
        ): FlowExternalOperation<TransactionReceipt> {
            require(isValidAddress(senderAddress)) { "Invalid senderAddress $receiverAddress" }
            require(isValidAddress(receiverAddress)) { "Invalid receiverAddress $receiverAddress" }

            return session.queueRemoteFunctionCall(token().transferFrom(senderAddress, receiverAddress, amount))
        }

        // non transactional functions

        override fun allowance(senderAddress: String, receiverAddress: String): FlowExternalOperation<BigInteger> {
            require(isValidAddress(senderAddress)) { "Invalid senderAddress $receiverAddress" }
            require(isValidAddress(receiverAddress)) { "Invalid receiverAddress $receiverAddress" }

            return session.simpleRemoteFunctionCall(token().allowance(senderAddress, receiverAddress))
        }

        override fun balanceOf(address: String): FlowExternalOperation<BigInteger> {
            require(isValidAddress(address)) { "Invalid address $address" }

            return session.simpleRemoteFunctionCall(token().balanceOf(address))
        }
    }

    /**
     * A wrapper to the [SwapVault] - [ISwapVault] pair: exposes [ISwapVault] and maps [SwapVault] calls to queueable
     * [FlowExternalOperation]s. Implements standard SwapVault interface:
     * https://ethereum.org/it/developers/docs/standards/tokens/erc-20/
     */
    private class SwapVaultWrapper(
        private val session: Session
    ) : ISwapVault {

        override val contractAddress = session.protocolAddress

        private fun swapVault() : SwapVault {
            return loadSwapVault(session.protocolAddress, session.connectionId, session as TxSignService)
        }

        override fun claimCommitment(swapId: String): FlowExternalOperation<TransactionReceipt> {
            return session.queueRemoteFunctionCall(swapVault().claimCommitment(swapId))
        }

        override fun revertCommitment(swapId: String): FlowExternalOperation<TransactionReceipt> {
            return session.queueRemoteFunctionCall(swapVault().revertCommitment(swapId))
        }

        override fun commit(
            swapId: String,
            recipient: String,
            signaturesThreshold: BigInteger
        ): FlowExternalOperation<TransactionReceipt> {
            return session.queueRemoteFunctionCall(swapVault().commit(swapId, recipient, signaturesThreshold))
        }

        override fun commitWithToken(
            swapId: String,
            tokenAddress: String,
            tokenId: BigInteger,
            amount: BigInteger,
            recipient: String,
            signaturesThreshold: BigInteger
        ): FlowExternalOperation<TransactionReceipt> {
            return session.queueRemoteFunctionCall(swapVault().commitWithToken(swapId, tokenAddress, tokenId, amount, recipient, signaturesThreshold))
        }

        override fun commitWithToken(
            swapId: String,
            tokenAddress: String,
            tokenId: BigInteger,
            amount: BigInteger,
            recipient: String,
            signaturesThreshold: BigInteger,
            signers: List<String>
        ): FlowExternalOperation<TransactionReceipt> {
            return session.queueRemoteFunctionCall(swapVault().commitWithToken(swapId, tokenAddress, tokenId, amount, recipient, signaturesThreshold, signers))
        }

        override fun commitWithToken(
            swapId: String,
            tokenAddress: String,
            amount: BigInteger,
            recipient: String,
            signaturesThreshold: BigInteger
        ): FlowExternalOperation<TransactionReceipt> {
            return session.queueRemoteFunctionCall(swapVault().commitWithToken(swapId, tokenAddress, amount, recipient, signaturesThreshold))
        }

        override fun commitWithToken(
            swapId: String,
            tokenAddress: String,
            amount: BigInteger,
            recipient: String,
            signaturesThreshold: BigInteger,
            signers: List<String>
        ): FlowExternalOperation<TransactionReceipt> {
            return session.queueRemoteFunctionCall(swapVault().commitWithToken(swapId, tokenAddress, amount, recipient, signaturesThreshold, signers))
        }
        override fun commitmentHash(swapId: String): FlowExternalOperation<ByteArray> {
            return session.simpleRemoteFunctionCall(swapVault().commitmentHash(swapId))
        }
    }

    /**
     * A wrapper to the [BridgeDeploy] - [IContracts] pair: exposes [IContracts] and maps [BridgeDeploy] calls
     * to queueable [FlowExternalOperation]s
     */
    private class ContractsWrapper(
        private val session: Session
    ) : IContracts {

        private fun contracts() : BridgeDeploy {
            return loadContracts(session.deployerAddress, session.connectionId, session as TxSignService)
        }

        override fun deployReverseERC20(chainId: BigInteger, salt: ByteArray, setup: TokenSetup): ResponseOperation<TransactionReceipt> {
            return deployReverseERC20(chainId, salt, initPayload(setup))
        }

        override fun deployReverseERC20(chainId: BigInteger, salt: ByteArray, initCode: ByteArray) : ResponseOperation<TransactionReceipt> {
            return session.queueRemoteFunctionCall(contracts().deployReverseERC20(chainId, salt, initCode))
        }

        override fun getReverseERC20Address(chainId: BigInteger, salt: ByteArray): FlowExternalOperation<ContractInfo> {
            val statusTransform = fun(s: Tuple2<Boolean, String>): ContractInfo {
                return ContractInfo(s.component1(), s.component2())
            }
            return session.simpleRemoteFunctionCall(contracts().getReverseERC20Address(salt), statusTransform)
        }

        private fun initPayload(tokenSetup: TokenSetup): ByteArray {
            val recipients = tokenSetup.balances.stream().map { Address(it.recipient) }.toList()
            val amounts = tokenSetup.balances.stream().map { Uint256(it.amount) }.toList()

            val payloadString = org.web3j.abi.DefaultFunctionEncoder().encodeParameters(
                arrayOf(
                    Utf8String(tokenSetup.tokenName),
                    Utf8String(tokenSetup.tokenSymbol),
                    DynamicArray<Address>(Address::class.java, recipients),
                    DynamicArray<Uint256>(Uint256::class.java, amounts)
                ).toList()
            )

            return Numeric.hexStringToByteArray(payloadString)
        }
    }

    /**
     * An implementation to some basic ETH/Ether functions (e.g. balanceOf)
     */
    private class EtherImpl(private val session: Session) : IEther {

        override fun balanceOf(address: String): FlowExternalOperation<BigInteger> {
            require(isValidAddress(address)) { "Invalid address $address" }

            val balance = session.getBalance(address, DefaultBlockParameterName.LATEST)
                .thenApply { it.balance }

            return ResponseOperation(balance)
        }
    }

    /**
     * An implementation to some basic Web3 functions. So far evmSetNextBlockTimestamp implemented, which is required
     * to set the clock on local test network deployments on environments that supports the functions.
     */
    private class Web3Impl(private val session: Session) : IWeb3 {

        override fun evmSetNextBlockTimestamp(timestamp: BigInteger) {
            try {
                session.ethCall("evm_setNextBlockTimestamp", listOf<String>(Numeric.encodeQuantity(timestamp))).getOrThrow()
                session.ethCall("evm_mine", emptyList<String>()).getOrThrow()
            } catch(ex: Exception) {
                throw ex
            }
        }

        override fun getEvents(address: String) : FlowExternalOperation<TransactionReceiptLog> {
            return session.queueRemoteEventLog(address)
        }

        override fun getBlockByNumber(number: BigInteger, fullTransactionObjects: Boolean) : FlowExternalOperation<Block> {
            val block = session.getBlockByNumber(number, fullTransactionObjects)

            return ResponseOperation(block)
        }

        override fun getBlockByHash(hash: String, fullTransactionObjects: Boolean) : FlowExternalOperation<Block> {
            val block = session.getBlockByHash(hash, fullTransactionObjects)

            return ResponseOperation(block)
        }

        override fun getTransactionByHash(hash: String) : FlowExternalOperation<com.r3.corda.evminterop.dto.Transaction> {
            val transaction = session.getTransactionByHash(hash)

            return ResponseOperation(transaction)
        }

        override fun getTransactionReceiptByHash(hash: String) : FlowExternalOperation<com.r3.corda.evminterop.dto.TransactionReceipt> {
            val transactionReceipt = session.getTransactionReceiptByHash(hash)

            return ResponseOperation(transactionReceipt)
        }


        override fun getBlockReceipts(blockNumber: BigInteger) : FlowExternalOperation<List<com.r3.corda.evminterop.dto.TransactionReceipt>> {
            val transactionReceipts = session.getBlockReceipts(blockNumber)

            return ResponseOperation(transactionReceipts)
        }
    }
}

fun FlowLogic<*>.evmInterop() : EvmBridge {
    return EvmBridge(this)
}

fun ServiceHub.evmInterop() : EvmBridge {
    return EvmBridge(this)
}

class EvmBridge(serviceHub: ServiceHub) {
    constructor(flowLogic: FlowLogic<*>) : this(flowLogic.serviceHub)

    private val owningKey = serviceHub.myInfo.legalIdentities.first().owningKey
    private val identityService = serviceHub.cordaService(IdentityServiceProvider::class.java)

    fun erc20Provider(tokenAddress: String): IERC20 = identityService.erc20(tokenAddress, owningKey)

    fun swapProvider(): ISwapVault = identityService.swap(owningKey)

    fun contractsProvider(): IContracts = identityService.contracts(owningKey)

    fun etherProvider(): IEther = identityService.ethers(owningKey)

    fun web3Provider(): IWeb3 = identityService.web3(owningKey)

    fun signerAddress() = identityService.signerAddress(owningKey)

    fun protocolAddress() = identityService.protocolAddress(owningKey)
}
