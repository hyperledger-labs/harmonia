package net.corda.samples.example.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import net.corda.samples.example.contracts.XVPContract;
import net.corda.samples.example.states.XVPState;

import java.security.SignatureException;
import java.util.Arrays;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class CreateXVPFlow {
	@InitiatingFlow
	@StartableByRPC
	public static class Initiator extends FlowLogic<SignedTransaction> {

		private final String tradeId;
		private final String assetId;
		private final Party sender;
		private final Party receiver;

		private final Step GENERATING_TRANSACTION = new Step("Generating transaction to record a new XvP trade.");
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

		public Initiator(String tradeId, String assetId, Party sender, Party receiver) {
			this.tradeId = tradeId;
			this.assetId = assetId;
			this.sender = sender;
			this.receiver = receiver;
		}

		@Override
		public ProgressTracker getProgressTracker() {
			return progressTracker;
		}

		@Suspendable
		@Override
		public SignedTransaction call() throws FlowException {

			// Obtain a reference to a notary we wish to use.
			final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
			if (notary == null) {
				throw new IllegalArgumentException("No notary by that name");
			}
			// Check the party running this flow.
			Party me = getOurIdentity();
			boolean isFromMeTrade = sender.getOwningKey().equals(me.getOwningKey());
			boolean isToMeTrade = receiver.getOwningKey().equals(me.getOwningKey());
			if (!isFromMeTrade && !isToMeTrade) {
				throw new IllegalArgumentException("The flow can only be run by one of the parties involved in the xvp trade");
			}
			// Generate an unsigned transaction.
			progressTracker.setCurrentStep(GENERATING_TRANSACTION);
			// Create the new xvp state
			XVPState xvpState = new XVPState(new UniqueIdentifier(), tradeId, assetId, sender, receiver, "CREATED");
			final Command<XVPContract.Commands.Create> txCommand = new Command<>(
				new XVPContract.Commands.Create(),
				Arrays.asList(sender.getOwningKey(), receiver.getOwningKey()));
			final TransactionBuilder txBuilder = new TransactionBuilder(notary)
				.addOutputState(xvpState, XVPContract.ID)
				.addCommand(txCommand);

			// Verify that the transaction is valid.
			progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
			txBuilder.verify(getServiceHub());

			// Sign the transaction.
			progressTracker.setCurrentStep(SIGNING_TRANSACTION);
			final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

			// Send the state to the counterparty, and receive it back with their signature.
			progressTracker.setCurrentStep(GATHERING_SIGS);
			FlowSession otherPartySession = initiateFlow(isFromMeTrade ? receiver : sender);
			final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

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
						XVPState trade = (XVPState) output;
						require.using("The output xvp state requires a receiver.", trade.getReceiver() != null);
						require.using("The output xvp state requires a sender.", trade.getSender() != null);
						require.using("The output xvp state requires a trade id.", trade.getTradeId() != null);
						Party me = getOurIdentity();
						boolean isFromMeTrade = trade.getSender().getOwningKey().equals(me.getOwningKey());
						boolean isToMeTrade = trade.getReceiver().getOwningKey().equals(me.getOwningKey());
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