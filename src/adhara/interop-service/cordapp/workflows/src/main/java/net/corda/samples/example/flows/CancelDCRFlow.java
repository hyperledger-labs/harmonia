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
import net.corda.samples.example.contracts.DCRContract;
import net.corda.samples.example.schema.DCRSchemaV1;
import net.corda.samples.example.schema.XVPSchemaV1;
import net.corda.samples.example.states.DCRState;
import net.corda.samples.example.states.XVPState;

import java.security.SignatureException;
import java.util.Arrays;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class CancelDCRFlow {
	@InitiatingFlow
	@StartableByRPC
	public static class Initiator extends FlowLogic<SignedTransaction> {

		private final String tradeId;
		private final String encodedInfo;
		private final String signatureOrProof;

		private final Step GENERATING_TRANSACTION = new Step("Generating transaction based on new DCR.");
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

		public Initiator(String tradeId, String encodedInfo, String signatureOrProof) {
			this.tradeId = tradeId;
			this.encodedInfo = encodedInfo;
			this.signatureOrProof = signatureOrProof;
		}

		@Override
		public ProgressTracker getProgressTracker() {
			return progressTracker;
		}

		@Suspendable
		@Override
		public SignedTransaction call() throws FlowException {
			// Retrieve the dcr State from the vault using Custom query
			QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
			// Get dcr information
			StateAndRef dcrInputStateAndRef = null;
			DCRState dcrInputStateToCancel = null;
			Party dcrNotary = null;
			try {
				FieldInfo dcrAttributeTradeId = QueryCriteriaUtils.getField("tradeId", DCRSchemaV1.PersistentDCR.class);
				CriteriaExpression dcrTradeIdIndex = Builder.equal(dcrAttributeTradeId, tradeId);
				QueryCriteria dcrTradeIdCriteria = new QueryCriteria.VaultCustomQueryCriteria(dcrTradeIdIndex);
				FieldInfo dcrAttributeStatus = QueryCriteriaUtils.getField("status", DCRSchemaV1.PersistentDCR.class);
				CriteriaExpression dcrStatusIndex = Builder.equal(dcrAttributeStatus, "EARMARKED");
				QueryCriteria dcrStatusCriteria = new QueryCriteria.VaultCustomQueryCriteria(dcrStatusIndex);
				QueryCriteria dcrCriteria = generalCriteria.and(dcrTradeIdCriteria).and(dcrStatusCriteria);
				Vault.Page<ContractState> dcrResults = getServiceHub().getVaultService().queryBy(DCRState.class, dcrCriteria);
				dcrInputStateAndRef = dcrResults.getStates().stream().findFirst().orElseThrow(() -> new FlowException("No dcr state with tradeId [" + tradeId + "] and status [EARMARKED] was found"));
				dcrInputStateToCancel = (DCRState) dcrInputStateAndRef.getState().getData();
				dcrNotary = dcrInputStateAndRef.getState().getNotary();
			} catch (NoSuchFieldException e) {
				throw new IllegalArgumentException("Error finding existing dcr states for trade id [" + tradeId + "]");
			}

			// Check the party running this flows is the issuer.
			if (!dcrInputStateToCancel.getIssuer().getOwningKey().equals(getOurIdentity().getOwningKey())) {
				throw new IllegalArgumentException("The issuer must issue the flow");
			}

			// Get xvp info.
			Vault.Page<ContractState> xvpResults = null;
			try {
				FieldInfo xvpAttributeTradeId = QueryCriteriaUtils.getField("tradeId", XVPSchemaV1.PersistentXVP.class);
				CriteriaExpression xvpTradeIdIndex = Builder.equal(xvpAttributeTradeId, tradeId);
				QueryCriteria xvpCustomCriteria = new QueryCriteria.VaultCustomQueryCriteria(xvpTradeIdIndex);
				QueryCriteria criteria = generalCriteria.and(xvpCustomCriteria);
				xvpResults = getServiceHub().getVaultService().queryBy(XVPState.class, criteria);
				if (xvpResults.getStates().isEmpty()) {
					throw new IllegalArgumentException("Found no xvp states for trade id [" + tradeId + "]");
				}
			} catch (NoSuchFieldException e) {
				throw new IllegalArgumentException("Error finding xvp states for trade id [" + tradeId + "]");
			}
			StateAndRef xvpInputStateAndRef = xvpResults.getStates()
				.stream()
				.findFirst().orElseThrow(() -> new FlowException("No xvp state with tradeId [" + tradeId + "] was found")); // Last unconsumed
			XVPState xvpInputStateToCheck = (XVPState) xvpInputStateAndRef.getState().getData();

			if (!xvpInputStateToCheck.getStatus().equals("CREATED")) {
				throw new IllegalArgumentException("The input states are not valid, expected xvp state [CREATED] but found [" + xvpInputStateToCheck.getStatus() + "]");
			}

			progressTracker.setCurrentStep(GENERATING_TRANSACTION);
			// Generate an unsigned transaction.
			// TODO: The proof should rather be be an attachment
			String proof = encodedInfo + "|" + signatureOrProof;
			DCRState outputStateToCancel = new DCRState(dcrInputStateToCancel.getValue(), dcrInputStateToCancel.getCurrency(), dcrInputStateToCancel.getIssuer(), dcrInputStateToCancel.getIssuer(), new UniqueIdentifier(), null, proof, "AVAILABLE");
			final Command<DCRContract.Commands.Cancel> txCommand = new Command<>(
				new DCRContract.Commands.Cancel(),
				Arrays.asList(dcrInputStateToCancel.getIssuer().getOwningKey(), dcrInputStateToCancel.getOwner().getOwningKey()));
			final TransactionBuilder txBuilder = new TransactionBuilder(dcrNotary)
				.addInputState(xvpInputStateAndRef)
				.addInputState(dcrInputStateAndRef)
				.addOutputState(outputStateToCancel, DCRContract.ID)
				.addCommand(txCommand);

			progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
			// Verify that the transaction is valid.
			txBuilder.verify(getServiceHub());

			progressTracker.setCurrentStep(SIGNING_TRANSACTION);
			// Sign the transaction.
			final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

			progressTracker.setCurrentStep(GATHERING_SIGS);
			// Send the state to the counterparty, and receive it back with their signature.
			FlowSession otherPartySession = initiateFlow(dcrInputStateToCancel.getOwner());
			final SignedTransaction fullySignedTx = subFlow(
				new CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

			progressTracker.setCurrentStep(FINALISING_TRANSACTION);
			// Notarise and record the transaction in both parties' vaults.
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
						require.using("The output state must be a dcr state.", output instanceof DCRState);
						DCRState dcr = (DCRState) output;
						require.using("The output dcr state requires a valid proof", dcr.getProof() != null && !dcr.getProof().isEmpty());
	  				// TODO: Add verification of the Ethereum Merkle Patricia proof
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
