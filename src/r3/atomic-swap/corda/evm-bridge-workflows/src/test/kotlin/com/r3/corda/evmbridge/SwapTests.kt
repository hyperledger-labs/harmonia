package com.r3.corda.evmbridge

import com.r3.corda.evmbridge.test.GenericAssetState
import com.r3.corda.evmbridge.test.IssueGenericAssetFlow
import com.r3.corda.evmbridge.internal.TestNetSetup
import com.r3.corda.evmbridge.services.swap.DraftTxService
import com.r3.corda.evmbridge.states.swap.LockState
import com.r3.corda.evmbridge.states.swap.SwapTransactionDetails
import com.r3.corda.evmbridge.states.swap.TransferEvent
import com.r3.corda.evmbridge.states.swap.TransferProof
import com.r3.corda.evmbridge.workflows.swap.BuildAndProposeDraftTransactionFlow
import com.r3.corda.evmbridge.workflows.swap.RequestBlockHeaderProofs
import com.r3.corda.evmbridge.workflows.swap.SignDraftTransactionFlow
import com.r3.corda.evmbridge.workflows.swap.UnlockTransactionAndObtainAssetFlow
import net.corda.core.contracts.OwnableState
import net.corda.core.crypto.SecureHash
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentity
import org.junit.Assert.assertEquals
import org.junit.Test

class SwapTests : TestNetSetup() {

    @Test
    fun successfulSwap() {
        // Create Corda asset to be swapped for EVM asset
        val future = bob.startFlow(IssueGenericAssetFlow())
        network?.runNetwork()
        future.getOrThrow()

        val asset = bob.services.vaultService.queryBy(GenericAssetState::class.java).states.single()

        // Generate swap transaction details. These details are shared by both swap parties and are used to coordinate
        // and identify events
        val swapDetails = SwapTransactionDetails(
            senderCordaName = bob.toParty(),
            senderEvmAddress = bobAddress,
            receiverCordaName = alice.toParty(),
            receiverEvmAddress = aliceAddress,
            cordaAssetState = asset,
            evmAssetContractAddress = goldTokenDeployAddress,
            approvedCordaValidators = listOf(charlie.toParty()),
            minimumNumberOfEventValidations = 1)

        // Build draft transaction and send it to counterparty for verification
        val future2 = bob.startFlow(BuildAndProposeDraftTransactionFlow(swapDetails, notary.toParty()))
        network?.runNetwork()
        val draftTx = future2.getOrThrow()
        assertEquals(draftTx!!.id, alice.services.cordaService(DraftTxService::class.java).getDraftTx(draftTx.id)!!.id)

        // Bob receives an event that Alice's EVM asset has been committed to the protocol and finalizes the draft tx
        val future3 = bob.startFlow(SignDraftTransactionFlow(draftTx))
        network?.runNetwork()
        future3.getOrThrow()

        // Alice initiates a forward transfer of the EVM asset and then unlocks the Corda asset to herself
        val lockedAsset = alice.services.vaultService.queryBy(GenericAssetState::class.java).states.single()
        val lockState = alice.services.vaultService.queryBy(LockState::class.java).states.single()

        // EVM transfer
        val transferEvent = TransferEvent(lockedAsset.ref.txhash, SecureHash.sha256("chainID" + "sig"))

        // Generate proof required for alice to take ownership of the Corda asset
        val future4 = alice.startFlow(RequestBlockHeaderProofs(lockedAsset.ref.txhash, swapDetails, listOf(charlie.toParty())))
        network?.runNetwork()
        val validatorSignatures = future4.getOrThrow()
        val proof = TransferProof(ByteArray(0), validatorSignatures, lockedAsset.ref.txhash, transferEvent)
        val future5 = alice.startFlow(UnlockTransactionAndObtainAssetFlow(lockedAsset, lockState, proof, notary.toParty()))
        network?.runNetwork()
        val stx = future5.getOrThrow()

        // Verify unlocked asset has new owner (Alice)
        assertEquals(alice.info.chooseIdentity().owningKey, (stx.tx.outputStates.single() as OwnableState).owner.owningKey)
        // Bob can't see the locked asset anymore
        assert(bob.services.vaultService.queryBy(GenericAssetState::class.java).states.isEmpty())
    }
}