package net.corda.samples.example.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.StatesToRecord;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.*;
import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria;
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import net.corda.samples.example.contracts.DCRContract;
import net.corda.samples.example.schema.DCRSchemaV1;
import net.corda.samples.example.schema.XVPSchemaV1;
import net.corda.samples.example.states.DCRState;
import net.corda.samples.example.states.XVPState;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.SignatureException;
import java.util.Arrays;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class ConfirmDCRFlow {
	@InitiatingFlow
	@StartableByRPC
	public static class Initiator extends FlowLogic<SignedTransaction> {

		private final String tradeId;
		private final String encodedInfo;
		private final String signatureOrProof;

		private final Step GENERATING_TRANSACTION = new Step("Generating transaction transfer an earmarked DCR.");
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

		private String uploadAttachment(String path, ServiceHub service, Party whoami, String filename) throws IOException {
			SecureHash attachmentHash = service.getAttachments().importAttachment(
				new FileInputStream(new File(path)),
				whoami.toString(),
				filename
			);
			return attachmentHash.toString();
		}

		@Suspendable
		@Override
		public SignedTransaction call() throws FlowException {
			// Retrieve the xvp and dcr state from the vault using Custom query
			QueryCriteria generalCriteria = new VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
			// Get dcr information
			StateAndRef dcrInputStateAndRef = null;
			DCRState dcrInputStateToConfirm = null;
			Party dcrNotary = null;
			try {
				FieldInfo dcrAttributeTradeId = QueryCriteriaUtils.getField("tradeId", DCRSchemaV1.PersistentDCR.class);
				CriteriaExpression dcrTradeIdIndex = Builder.equal(dcrAttributeTradeId, tradeId);
				QueryCriteria dcrTradeIdCriteria = new VaultCustomQueryCriteria(dcrTradeIdIndex);
				FieldInfo dcrAttributeStatus = QueryCriteriaUtils.getField("status", DCRSchemaV1.PersistentDCR.class);
				CriteriaExpression dcrStatusIndex = Builder.equal(dcrAttributeStatus, "EARMARKED");
				QueryCriteria dcrStatusCriteria = new VaultCustomQueryCriteria(dcrStatusIndex);
				QueryCriteria dcrCriteria = generalCriteria.and(dcrTradeIdCriteria).and(dcrStatusCriteria);
				Vault.Page<ContractState> dcrResults = getServiceHub().getVaultService().queryBy(DCRState.class, dcrCriteria);
				dcrInputStateAndRef = dcrResults.getStates().stream().findFirst().orElseThrow(() -> new FlowException("No dcr state with tradeId [" + tradeId + "] and status [EARMARKED] was found"));
				dcrInputStateToConfirm = (DCRState) dcrInputStateAndRef.getState().getData();
				dcrNotary = dcrInputStateAndRef.getState().getNotary();
			} catch (NoSuchFieldException e) {
				throw new IllegalArgumentException("Error finding existing dcr states for trade id [" + tradeId + "]");
			}

			// Check the party running this flows is the issuer of the dcr.
			if (!dcrInputStateToConfirm.getIssuer().getOwningKey().equals(getOurIdentity().getOwningKey())) {
				throw new IllegalArgumentException("The issuer must issue this flow");
			}

			// Get xvp information
			StateAndRef xvpInputStateAndRef = null;
			XVPState xvpInputStateToConfirm = null;
			Party xvpNotary = null;
			try {
				FieldInfo xvpAttributeTradeId = QueryCriteriaUtils.getField("tradeId", XVPSchemaV1.PersistentXVP.class);
				CriteriaExpression xvpTradeIdIndex = Builder.equal(xvpAttributeTradeId, tradeId);
				QueryCriteria xvpTradeIdCriteria = new VaultCustomQueryCriteria(xvpTradeIdIndex);
				FieldInfo xvpStatusId = QueryCriteriaUtils.getField("status", XVPSchemaV1.PersistentXVP.class);
				CriteriaExpression xvpStatusIndex = Builder.equal(xvpStatusId, "CREATED");
				QueryCriteria xvpStatusCriteria = new VaultCustomQueryCriteria(xvpStatusIndex);
				QueryCriteria xvpCriteria = generalCriteria.and(xvpTradeIdCriteria).and(xvpStatusCriteria);
				Vault.Page<ContractState> xvpResults = getServiceHub().getVaultService().queryBy(XVPState.class, xvpCriteria);
				xvpInputStateAndRef = xvpResults.getStates().stream().findFirst().orElseThrow(() -> new FlowException("No xvp state with tradeId [" + tradeId + "] was found"));
				xvpInputStateToConfirm = (XVPState) xvpInputStateAndRef.getState().getData();
				xvpNotary = xvpInputStateAndRef.getState().getNotary();
			} catch (NoSuchFieldException e) {
				throw new IllegalArgumentException("Error finding existing states for trade id [" + tradeId + "]");
			}

			// Check the notaries
			if (!dcrNotary.getOwningKey().equals(xvpNotary.getOwningKey())) {
				throw new IllegalArgumentException("The dcr state and xvp state have different notaries");
			}

			// Check the xvp state sender and receiver match dcr issuer and owner
			if (!xvpInputStateToConfirm.getSender().getOwningKey().equals(dcrInputStateToConfirm.getIssuer().getOwningKey()) ||
			    !xvpInputStateToConfirm.getReceiver().getOwningKey().equals(dcrInputStateToConfirm.getOwner().getOwningKey())) {
				throw new IllegalArgumentException("The parties specified in the xvp state do not match the parties specified in the dcr state");
			}

			// Construct new dcr state
			String nextState = "TRANSFERRED";
			// TODO: The proof should be an attachment ideally
			String proof = encodedInfo + "|" + signatureOrProof;
			if (proof.startsWith("|") || proof.endsWith("|")) {
				throw new IllegalArgumentException("Detected malformed proof when confirming a xvp trade");
			}

			// Generate an unsigned transaction.
			progressTracker.setCurrentStep(GENERATING_TRANSACTION);
			DCRState dcrOutputStateToConfirm = new DCRState(dcrInputStateToConfirm.getValue(), dcrInputStateToConfirm.getCurrency(), dcrInputStateToConfirm.getOwner(), dcrInputStateToConfirm.getIssuer(), new UniqueIdentifier(), dcrInputStateToConfirm.getTradeId(), proof, nextState);
			final Command<DCRContract.Commands.Confirm> txCommand = new Command<>(
				new DCRContract.Commands.Confirm(),
				Arrays.asList(dcrOutputStateToConfirm.getIssuer().getOwningKey(), dcrOutputStateToConfirm.getOwner().getOwningKey()));
			final TransactionBuilder txBuilder = new TransactionBuilder(dcrNotary)
				.addInputState(xvpInputStateAndRef)
				.addInputState(dcrInputStateAndRef)
				.addOutputState(dcrOutputStateToConfirm, DCRContract.ID)
				.addCommand(txCommand);

			// Verify that the transaction is valid.
			progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
			txBuilder.verify(getServiceHub());

			// Sign the transaction.
			progressTracker.setCurrentStep(SIGNING_TRANSACTION);
			final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

			// Send the state to the counterparty, and receive it back with their signature.
			progressTracker.setCurrentStep(GATHERING_SIGS);
			FlowSession otherPartySession = initiateFlow(dcrInputStateToConfirm.getOwner());
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
						require.using("The output state must be a dcr state", output instanceof DCRState);
						DCRState dcr = (DCRState) output;
						require.using("The output dcr state requires a valid proof", dcr.getProof() != null && !dcr.getProof().isEmpty());
						Party me = getOurIdentity();
						boolean isForMe = dcr.getOwner().getOwningKey().equals(me.getOwningKey());
						require.using("The flow can only be run by the owner", isForMe);
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
