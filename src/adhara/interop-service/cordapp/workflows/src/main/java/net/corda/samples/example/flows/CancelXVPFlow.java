package net.corda.samples.example.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import net.corda.samples.example.contracts.XVPContract;
import net.corda.samples.example.schema.DCRSchemaV1;
import net.corda.samples.example.schema.XVPSchemaV1;
import net.corda.samples.example.schema.XVPSchemaV1;
import net.corda.samples.example.states.DCRState;
import net.corda.samples.example.states.XVPState;
import net.corda.samples.example.states.XVPState;

import java.security.SignatureException;
import java.util.Arrays;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class CancelXVPFlow {
	@InitiatingFlow
	@StartableByRPC
	public static class Initiator extends FlowLogic<SignedTransaction> {

		private final String tradeId;

		private final Step GENERATING_TRANSACTION = new Step("Generating transaction to cancel a XvP trade.");
		private final Step VERIFYING_TRANSACTION = new Step("Verifying contract constraints.");
		private final Step SIGNING_TRANSACTION = new Step("Signing transaction with our private key.");
		private final Step GATHERING_SIGS = new Step("Gathering the counter party's signature.") {
			@Override
			public ProgressTracker childProgressTracker() {
				return CollectSignaturesFlow.Companion.tracker();
			}
		};
		private final Step FINALISING_TRANSACTION = new Step("Obtaining notary signature and recording transaction.") {
			@Override
			public ProgressTracker childProgressTracker() {
				return FinalityFlow.Companion.tracker();
			}
		};

		private final ProgressTracker progressTracker = new ProgressTracker(
			GENERATING_TRANSACTION,
			VERIFYING_TRANSACTION,
			SIGNING_TRANSACTION,
			GATHERING_SIGS,
			FINALISING_TRANSACTION
		);

		public Initiator(String tradeId) {
			this.tradeId = tradeId;
		}

		@Override
		public ProgressTracker getProgressTracker() {
			return progressTracker;
		}

		@Suspendable
		@Override
		public SignedTransaction call() throws FlowException {
			// Retrieve the dcr and xvp states from the vault using Custom query for trade id.
			QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
			// Get dcr information
			try {
				FieldInfo dcrAttributeTradeId = QueryCriteriaUtils.getField("tradeId", DCRSchemaV1.PersistentDCR.class);
				CriteriaExpression dcrTradeIdIndex = Builder.equal(dcrAttributeTradeId, tradeId);
				QueryCriteria dcrTradeIdCriteria = new QueryCriteria.VaultCustomQueryCriteria(dcrTradeIdIndex);
				FieldInfo dcrAttributeStatus = QueryCriteriaUtils.getField("status", DCRSchemaV1.PersistentDCR.class);
				CriteriaExpression dcrStatusIndex = Builder.equal(dcrAttributeStatus, "EARMARKED");
				QueryCriteria dcrStatusCriteria = new QueryCriteria.VaultCustomQueryCriteria(dcrStatusIndex);
				QueryCriteria dcrCriteria = generalCriteria.and(dcrTradeIdCriteria).and(dcrStatusCriteria);
				Vault.Page<ContractState> dcrResults = getServiceHub().getVaultService().queryBy(DCRState.class, dcrCriteria);
				if (!dcrResults.getStates().isEmpty()) {
					throw new FlowException("Found earmarked dcr state with tradeId [" + tradeId + "]");
				}
			} catch (NoSuchFieldException e) {
				throw new IllegalArgumentException("Error finding existing dcr states for trade id [" + tradeId + "]");
			}
			// Get xvp information
			StateAndRef xvpInputStateAndRef = null;
			XVPState xvpInputStateToCancel = null;
			Party xvpNotary = null;
			try {
				FieldInfo xvpAttributeTradeId = QueryCriteriaUtils.getField("tradeId", XVPSchemaV1.PersistentXVP.class);
				CriteriaExpression xvpTradeIdIndex = Builder.equal(xvpAttributeTradeId, tradeId);
				QueryCriteria xvpTradeIdCriteria = new QueryCriteria.VaultCustomQueryCriteria(xvpTradeIdIndex);
				FieldInfo xvpStatusId = QueryCriteriaUtils.getField("status", XVPSchemaV1.PersistentXVP.class);
				CriteriaExpression xvpStatusIndex = Builder.equal(xvpStatusId, "CREATED");
				QueryCriteria xvpStatusCriteria = new QueryCriteria.VaultCustomQueryCriteria(xvpStatusIndex);
				QueryCriteria xvpCriteria = generalCriteria.and(xvpTradeIdCriteria).and(xvpStatusCriteria);
				Vault.Page<ContractState> xvpResults = getServiceHub().getVaultService().queryBy(XVPState.class, xvpCriteria);
				xvpInputStateAndRef = xvpResults.getStates().stream().findFirst().orElseThrow(() -> new FlowException("No xvp state with tradeId [" + tradeId + "] and status [CREATED] was found"));
				xvpInputStateToCancel = (XVPState) xvpInputStateAndRef.getState().getData();
				xvpNotary = xvpInputStateAndRef.getState().getNotary();
			} catch (NoSuchFieldException e) {
				throw new IllegalArgumentException("Error finding existing states for trade id [" + tradeId + "]");
			}

			// Check the party running this flow.
			Party me = getOurIdentity();
			boolean isFromMeTrade = xvpInputStateToCancel.getSender().getOwningKey().equals(me.getOwningKey());
		  boolean isToMeTrade = xvpInputStateToCancel.getReceiver().getOwningKey().equals(me.getOwningKey());
			if (!isFromMeTrade && !isToMeTrade) {
				throw new IllegalArgumentException("The flow can only be run by one of the parties involved in the xvp trade");
			}

			// Generate an unsigned transaction.
			progressTracker.setCurrentStep(GENERATING_TRANSACTION);
			XVPState outputStateToCancel = new XVPState(new UniqueIdentifier(), xvpInputStateToCancel.getTradeId(), xvpInputStateToCancel.getAssetId(), xvpInputStateToCancel.getSender(), xvpInputStateToCancel.getReceiver(), "CANCELLED");
			final Command<XVPContract.Commands.Cancel> txCommand = new Command<>(
				new XVPContract.Commands.Cancel(),
				Arrays.asList(outputStateToCancel.getSender().getOwningKey(), outputStateToCancel.getReceiver().getOwningKey()));
			final TransactionBuilder txBuilder = new TransactionBuilder(xvpNotary)
				.addInputState(xvpInputStateAndRef)
				.addOutputState(outputStateToCancel, XVPContract.ID)
				.addCommand(txCommand);

			// Verify that the transaction is valid.
			progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
			txBuilder.verify(getServiceHub());

			// Sign the transaction.
			progressTracker.setCurrentStep(SIGNING_TRANSACTION);
			final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

			// Send the state to the counterparty, and receive it back with their signature.
			progressTracker.setCurrentStep(GATHERING_SIGS);
			FlowSession otherPartySession = initiateFlow(isFromMeTrade ? xvpInputStateToCancel.getReceiver() : xvpInputStateToCancel.getSender());
			final SignedTransaction fullySignedTx = subFlow(
				new CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

			// Notarise and record the transaction in both parties' vaults.
			progressTracker.setCurrentStep(FINALISING_TRANSACTION);
			return subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession), StatesToRecord.ALL_VISIBLE));
		}
	}

	@InitiatedBy(Initiator.class)
	public static class Acceptor extends FlowLogic<SignedTransaction> {

		private final FlowSession otherPartySession;

		public Acceptor(FlowSession otherPartySession) {
			this.otherPartySession = otherPartySession;
		}

		@Suspendable
		@Override
		public SignedTransaction call() throws FlowException {
			class SignTxFlow extends SignTransactionFlow {
				private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
					super(otherPartyFlow, progressTracker);
				}

				@Override
				protected void checkTransaction(SignedTransaction stx) {
					try {
						stx.verify(getServiceHub());
					} catch (IllegalArgumentException | SignatureException | AttachmentResolutionException | TransactionResolutionException | TransactionVerificationException e) {
						return;
					}
					requireThat(require -> {
						ContractState output = stx.getTx().getOutputs().get(0).getData();
						require.using("The output state must be a xvp state.", output instanceof XVPState);
						XVPState xvp = (XVPState) output;
						Party me = getOurIdentity();
						boolean isFromMeTrade = xvp.getSender().getOwningKey().equals(me.getOwningKey());
						boolean isToMeTrade = xvp.getReceiver().getOwningKey().equals(me.getOwningKey());
						require.using("The flow can only be run by one of the parties involved in the xvp trade", isFromMeTrade || isToMeTrade);
						return null;
					});
				}
			}
			final SignTxFlow signTxFlow = new SignTxFlow(otherPartySession, SignTransactionFlow.Companion.tracker());
			final SecureHash txId = subFlow(signTxFlow).getId();

			return subFlow(new ReceiveFinalityFlow(otherPartySession, txId));
		}
	}
}
