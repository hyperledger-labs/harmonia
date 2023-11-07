package org.web3j.generated.contracts

import net.corda.core.flows.FlowExternalOperation
import java.math.BigInteger

/**
 * The interface to the EVM Swap contract that allows commit and claim-or-revert of EVM assets
 */
interface ISwapVault {

    /**
     * Represent the contract's current deployment address
     */
    val contractAddress: String

    /**
     * Claim a commitment (forward the committed asset to the recipient). This can only be executed by the
     * original committed asset owner (asset committer).
     */
    fun claimCommitment(swapId: String): FlowExternalOperation<com.r3.corda.evminterop.dto.TransactionReceipt>

    /**
     * Claim a commitment (forward the committed asset to the recipient). This can be executed by the recipient of the
     * committed asset, by presenting enough signatures that attest the Corda draft transaction was notarized by the
     * expected notary.
     */
    fun claimCommitment(swapId: String, signatures: List<ByteArray>): FlowExternalOperation<com.r3.corda.evminterop.dto.TransactionReceipt>

    /**
     * Revert the committed asset back to the original owner. Revert can be executed by either the original owner
     * (committer) or the recipient.
     */
    fun revertCommitment(swapId: String): FlowExternalOperation<com.r3.corda.evminterop.dto.TransactionReceipt>

    /**
     * Currently unused
     */
    fun commit(
        swapId: String,
        recipient: String,
        signaturesThreshold: BigInteger
    ): FlowExternalOperation<com.r3.corda.evminterop.dto.TransactionReceipt>

    /**
     * Commit an ERC721 or ERC1155 token to the contract without locking the asset (can be reverted with no restrictions).
     *
     * @param swapId The hash of the draft transaction that will be locked once notarized
     * @param tokenAddress the address of the asset that is going to be committed
     * @param tokenId the tokenId of the asset that is going to be committed
     * @param amount the amount of tokens to commit
     * @param recipient the expected recipient that shall receive the asset if claimed successfully
     * @param signaturesThreshold the amount of signatures that are required in order to unlock the locked asset on Corda
     */
    fun commitWithToken(
        swapId: String,
        tokenAddress: String,
        tokenId: BigInteger,
        amount: BigInteger,
        recipient: String,
        signaturesThreshold: BigInteger
    ): FlowExternalOperation<com.r3.corda.evminterop.dto.TransactionReceipt>

    /**
     * Commit an ERC721 or ERC1155 token to the contract without locking the asset (can be reverted with no restrictions).
     *
     * @param swapId The hash of the draft transaction that will be locked once notarized
     * @param tokenAddress the address of the asset that is going to be committed
     * @param tokenId the tokenId of the asset that is going to be committed
     * @param amount the amount of tokens to commit
     * @param recipient the expected recipient that shall receive the asset if claimed successfully
     * @param signaturesThreshold the amount M of signatures that are required in order to unlock the locked asset on Corda
     * @param signers the EVM addresses of the N signers (with N >= M) whose signatures would be required by the recipient
     *                of the EVM asset in order to claim the asset successfully for himself.
     */
    fun commitWithToken(
        swapId: String,
        tokenAddress: String,
        tokenId: BigInteger,
        amount: BigInteger,
        recipient: String,
        signaturesThreshold: BigInteger,
        signers: List<String>
    ): FlowExternalOperation<com.r3.corda.evminterop.dto.TransactionReceipt>

    /**
     * Commit an ERC20 token to the contract without locking the asset (can be reverted with no restrictions).
     *
     * @param swapId The hash of the draft transaction that will be locked once notarized
     * @param tokenAddress the address of the asset that is going to be committed
     * @param amount the amount of tokens to commit
     * @param recipient the expected recipient that shall receive the asset if claimed successfully
     * @param signaturesThreshold the amount M of signatures that are required in order to unlock the locked asset on Corda
     */
    fun commitWithToken(
        swapId: String,
        tokenAddress: String,
        amount: BigInteger,
        recipient: String,
        signaturesThreshold: BigInteger
    ): FlowExternalOperation<com.r3.corda.evminterop.dto.TransactionReceipt>

    /**
     * Commit an ERC20 token to the contract without locking the asset (can be reverted with no restrictions).
     *
     * @param swapId The hash of the draft transaction that will be locked once notarized
     * @param tokenAddress the address of the asset that is going to be committed
     * @param amount the amount of tokens to commit
     * @param recipient the expected recipient that shall receive the asset if claimed successfully
     * @param signaturesThreshold the amount M of signatures that are required in order to unlock the locked asset on Corda
     * @param signers the EVM addresses of the N signers (with N >= M) whose signatures would be required by the recipient
     *                of the EVM asset in order to claim the asset successfully for himself.
     */
    fun commitWithToken(
        swapId: String,
        tokenAddress: String,
        amount: BigInteger,
        recipient: String,
        signaturesThreshold: BigInteger,
        signers: List<String>
    ): FlowExternalOperation<com.r3.corda.evminterop.dto.TransactionReceipt>

    /**
     * Retrieve the commitment hash that would be produced by a claim or revert event for the given swap id.
     */
    fun commitmentHash(swapId: String): FlowExternalOperation<ByteArray>
}
