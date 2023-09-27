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
import net.corda.samples.example.schema.XVPSchemaV1;
import net.corda.samples.example.states.DCRState;
import net.corda.samples.example.states.XVPState;

import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class ResolveXVPFlow {
	@InitiatingFlow
	@StartableByRPC
	public static class Initiator extends FlowLogic<SignedTransaction> {

		private final String tradeId;

		private final Step GENERATING_TRANSACTION = new Step("Generating transaction to resolve a cancelled XVP.");
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
			// Retrieve the DCR State from the vault using Custom query
			QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
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
			XVPState xvpInputStateToResolve = (XVPState) xvpInputStateAndRef.getState().getData();

			// Check the states.
			if (!xvpInputStateToResolve.getStatus().equals("CANCELLED"))	{
				throw new IllegalArgumentException("The input states are not valid, expected xvp state [CANCELLED] but found [" + xvpInputStateToResolve.getStatus() + "]");
			}
			// Check the party running this flow.
			Party me = getOurIdentity();
			boolean isFromMeTrade = xvpInputStateToResolve.getSender().getOwningKey().equals(me.getOwningKey());
			boolean isToMeTrade = xvpInputStateToResolve.getReceiver().getOwningKey().equals(me.getOwningKey());
			if (!isFromMeTrade && !isToMeTrade) {
				throw new IllegalArgumentException("The flow can only be run by one of the parties involved in the xvp trade");
			}

			// Obtain a reference to a notary we wish to use.
			Party notary = xvpInputStateAndRef.getState().getNotary();
			// Build a new transaction
			progressTracker.setCurrentStep(GENERATING_TRANSACTION);
			final Command<XVPContract.Commands.Resolve> txCommand = new Command<>(
				new XVPContract.Commands.Resolve(),
				Arrays.asList(xvpInputStateToResolve.getReceiver().getOwningKey(), xvpInputStateToResolve.getSender().getOwningKey()));
			final TransactionBuilder txBuilder = new TransactionBuilder(notary)
				.addInputState(xvpInputStateAndRef)
				.addCommand(txCommand);

			// Verify that the transaction is valid.
			progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
			txBuilder.verify(getServiceHub());

			// Sign the transaction.
			progressTracker.setCurrentStep(SIGNING_TRANSACTION);
			final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

			// Send the state to the counterparty, and receive it back with their signature.
			progressTracker.setCurrentStep(GATHERING_SIGS);
			FlowSession otherPartySession = initiateFlow(isFromMeTrade ? xvpInputStateToResolve.getReceiver() : xvpInputStateToResolve.getSender());
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
						require.using("There must be no output states", stx.getTx().getOutputs().isEmpty());
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
