package com.interop.flows

import com.interop.flows.internal.TestNetSetup
import com.r3.corda.evminterop.SwapVaultEventEncoder
import com.r3.corda.evminterop.dto.TransactionReceipt
import com.r3.corda.evminterop.services.swap.DraftTxService
import com.r3.corda.evminterop.workflows.GenericAssetSchemaV1
import com.r3.corda.evminterop.workflows.GenericAssetState
import com.r3.corda.evminterop.workflows.IssueGenericAssetFlow
import com.r3.corda.evminterop.workflows.swap.ClaimCommitmentWithSignatures
import com.r3.corda.evminterop.workflows.swap.CommitWithTokenFlow
import com.r3.corda.evminterop.workflows.swap.GetBalanceFlow
import net.corda.core.contracts.OwnableState
import net.corda.core.flows.FlowLogic
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.StartedMockNode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*

class SwapDemo : TestNetSetup() {

    companion object {
        private val BIG_INT_ONE = BigInteger.ONE
        private val BIG_INT_TWO = 2.toBigInteger()
    }

    /*
    Dramatis personae:

    * Alice, who holds tokens in an ERC20 contract on the EVM network, and wishes to use them to purchase an asset on
      the Corda network.
    * Bob, who holds the asset to be purchased.
    * Charlie and Dave, who are acting as trusted relays attesting to the finality of transactions on each network.
    * The Notary, which signs transactions on the Corda network (but using a non-EVM-native signature scheme).

    Each of Alice, Bob, Charlie and Dave has an identity on the Corda network - we write these as Alice@Corda, Bob@Corda
    etc - and a wallet/signing key on the EVM network, which we write as Alice@EVM, Bob@EVM etc.

    All actions in the swap are taken by running Corda flows. Some of these flows create, distribute and finalise
    transactions on the Corda network; some of them use the signing key for the corresponding EVM wallet to propose
    transactions to the EVM network. They also obtain attestations from the relays, Charlie and Dave.

    Given that Alice and Bob have already agreed to make the swap, the sequence is as follows:

    1. Bob drafts a Corda transaction which, if signed by him and the notary, will transfer ownership of the asset to
        Alice.
    2. Alice@Corda, having verified that the draft transaction will deliver the benefit she wants, commits tokens on the
        EVM network:
        2.1 Alice@EVM uses the ERC20 allow function to allocates the tokens to an allowance held by the SwapVault contract.
        2.2 Alice@EVM uses the SwapVault contract uses the commitWithToken function to transfer that allowance to the
            SwapVault contract itself, simultaneously setting up a Commit state which records the conditions under which
            the tokens can be either returned to Alice@EVM or transferred to Bob@EVM.
     3. Bob@Corda signs and notarises the draft transaction.
     4. Bob@Corda collects EVM-checkable signatures (on the draft transaction hash) from Charlie and Dave, attesting that
        they have observed and validated the notary's signature on that transaction hash is valid.
     5. Bob@EVM transfers the tokens to himself using the transfer function on SwapVault, presenting these signatures.
     6. Alice@Corda collects signatures from Charlie and Dave on the block hash of the transaction block containing the
        transfer transaction, attesting that they have observed it to be finalised on the EVM network.
     7. Alice@Corda runs UnlockAssetFlow which uses these signatures, together with a Merkle Patricia inclusion proof
        showing that the Transfer event is included in the event log of the transaction receipt for the transaction, to
        satisfy the Corda contract verification rules for unlocking ownership of the Corda asset to herself.
     */
    @Test
    fun `bob can transfer evm asset by asynchronous collection of notarisation signatures`() {

        /*
        Create a Corda asset owned by Bob.

        The txHash and stateIndex together uniquely identify the output state of the issuance transaction, which will
        be consumed by the transfer transaction which changes ownership of the state to Alice.
        */
        val assetName = UUID.randomUUID().toString()
        val (txHash, stateIndex) = bob.awaitFlow(IssueGenericAssetFlow(assetName))

        /*
        Prepare the generic claim / revert` event expectation.

        Note that this is not the encoded event but the event encoder. It does not include the draft transaction hash,
        which is only known after the draft transaction is created.

        Therefore, the encoder builds the encoded event only when a new transaction consumes the draft transaction
        outputs, using their state reference (txHash, stateIndex) to build the full encoded event.
        */
        val swapVaultEventEncoder = SwapVaultEventEncoder.create(
            chainId = BigInteger.valueOf(1337),
            protocolAddress = protocolAddress,
            owner = aliceAddress,
            recipient = bobAddress,
            amount = BIG_INT_ONE,
            tokenId = BigInteger.ZERO,
            tokenAddress = goldTokenDeployAddress,
            signaturesThreshold = BIG_INT_TWO,
            signers = listOf(charlieAddress, daveAddress)
        )

        /*
        1. Draft the Corda Asset transfer that can be transferred to the recipient or reverted to the owner if valid
        EVM event proofs are presented for the claim / revert transaction events from the expected protocol address
        and draft transaction hash (swap id).
        */
        val draftTxHash = bob.awaitFlow(
            DraftAssetSwapFlow(
            txHash,         // state ref of the asset to be transferred
            stateIndex,     //
            alice.toParty(),    // receiver of the asset
            notary.toParty(),   // notariser of the tx
            listOf(charlie.toParty(), dave.toParty()),  // witnesses to the notary signature
            2,  // threshold of proof that the notary signature is value - here, both witnesses
            swapVaultEventEncoder   // Defines the expected EVM events that must be proven for transfer/revert to be valid
        ))

        printBalances("before commit")

        /*
        2. Alice commits her tokens to the protocol contract.
         */
        val commitTxReceipt: TransactionReceipt = alice.awaitFlow(
            CommitWithTokenFlow(
                draftTxHash,
                goldTokenDeployAddress,
                BIG_INT_ONE,
                bobAddress,
                2.toBigInteger(),
                listOf(charlieAddress, daveAddress))
        )

        printBalances("after commit")

        /*
        3. Bob signs and notarises the draft transaction.
         */
        val stx = bob.awaitFlow(SignDraftTransactionByIDFlow(draftTxHash))

        /*
        4. Bob collects EVM-readable signatures from Charlie and Dave attesting that the draft transaction was notarised
         */
        bob.awaitFlow(CollectNotarizationSignaturesFlow(draftTxHash, true))
        val signatures = bob.services.cordaService(DraftTxService::class.java).notarizationProofs(draftTxHash)

        /*
        5. Bob uses the collected EVM-readable signatures to claim Alice's EVM committed asset
         */
        val txReceipt: TransactionReceipt = bob.awaitFlow(
            ClaimCommitmentWithSignatures(draftTxHash, signatures)
        )

        printBalances("after transfer")

        /*
        6. Alice collects signatures from Charlie and Dave attesting to the finality of the block containing the claim's
        transfer event.
        */
        alice.awaitFlow(CollectBlockSignaturesFlow(draftTxHash, txReceipt.blockNumber, true))

        /*
        7. Alice unlocks and finalizes the transfer to the recipient by producing and presenting proofs that the EVM
        tokens were transferred to the expected recipient.
        */
        val utx = alice.awaitFlow(
            UnlockAssetFlow(
                stx.tx.id,
                txReceipt.blockNumber,
                Numeric.toBigInt(txReceipt.transactionIndex!!)
            )
        )

        // Verify the unlocked asset is now owned by Alice and not anymore from Bob
        assertEquals(
            alice.info.chooseIdentity().owningKey,
            (utx.tx.outputStates.single() as OwnableState).owner.owningKey
        )

        // Verify that bob can't see the locked asset anymore
        assert(bob.services.vaultService.queryBy(GenericAssetState::class.java, queryCriteria(assetName)).states.isEmpty())
    }


    private fun printBalances(label: String) {
        val aliceBalance = alice.awaitFlow(GetBalanceFlow(goldTokenDeployAddress, aliceAddress))
        val bobBalance = bob.awaitFlow(GetBalanceFlow(goldTokenDeployAddress, bobAddress))
        val protocolBalance = bob.awaitFlow(GetBalanceFlow(goldTokenDeployAddress, protocolAddress))

        println("Balances $label: Alice = $aliceBalance, Bob = $bobBalance, protocol = $protocolBalance")
    }

    private fun <T> StartedMockNode.awaitFlow(flow: FlowLogic<T>): T = await(this.startFlow(flow))

    private fun queryCriteria(assetName: String): QueryCriteria.VaultCustomQueryCriteria<GenericAssetSchemaV1.PersistentGenericAsset> {
        return builder {
            QueryCriteria.VaultCustomQueryCriteria(
                GenericAssetSchemaV1.PersistentGenericAsset::assetName.equal(
                    assetName
                )
            )
        }
    }
}