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
import net.corda.samples.example.states.DCRState;

import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class EarmarkDCRFlow {
	@InitiatingFlow
	@StartableByRPC
	public static class Initiator extends FlowLogic<SignedTransaction> {

		private final String linearId;
		private final Party otherParty;
		private final String tradeId;

		private final Step GENERATING_TRANSACTION = new Step("Generating transaction to earmark an existing DCR.");
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

		public Initiator(String linearId, Party otherParty, String tradeId) {
			this.linearId = linearId;
			this.otherParty = otherParty;
			this.tradeId = tradeId;
		}

		@Override
		public ProgressTracker getProgressTracker() {
			return progressTracker;
		}

		@Suspendable
		@Override
		public SignedTransaction call() throws FlowException {
			// Retrieve the DCR State from the vault using LinearStateQueryCriteria
			List<UUID> listOfLinearIds = Arrays.asList(UUID.fromString(linearId));
			QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, listOfLinearIds);
			Vault.Page<ContractState> results = getServiceHub().getVaultService().queryBy(DCRState.class, queryCriteria);
			StateAndRef<ContractState> inputStateAndRef = results.getStates()
				.stream()
				.findFirst().orElseThrow(() -> new FlowException("No state with linear id [" + linearId + "] was found"));

			DCRState inputStateToEarmark = (DCRState) inputStateAndRef.getState().getData();
			if (!inputStateToEarmark.getIssuer().getOwningKey().equals(getOurIdentity().getOwningKey())) {
				throw new IllegalArgumentException("This flow can only be run by the issuer");
			}
			if (!inputStateToEarmark.getStatus().equals("AVAILABLE")) {
				throw new IllegalArgumentException("The input state is not valid, expected [AVAILABLE] but found [" + inputStateToEarmark.getStatus() + "]");
			}

			// Obtain a reference to a notary we wish to use.
			Party notary = inputStateAndRef.getState().getNotary();

			progressTracker.setCurrentStep(GENERATING_TRANSACTION);
			DCRState outputStateToEarmark = new DCRState(inputStateToEarmark.getValue(), inputStateToEarmark.getCurrency(), otherParty, inputStateToEarmark.getIssuer(), new UniqueIdentifier(), tradeId, "", "EARMARKED");
			final Command<DCRContract.Commands.Earmark> txCommand = new Command<>(
				new DCRContract.Commands.Earmark(),
				Arrays.asList(outputStateToEarmark.getIssuer().getOwningKey(), outputStateToEarmark.getOwner().getOwningKey()));
			final TransactionBuilder txBuilder = new TransactionBuilder(notary)
				.addInputState(inputStateAndRef)
				.addOutputState(outputStateToEarmark, DCRContract.ID)
				.addCommand(txCommand);

			progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
			// Verify that the transaction is valid.
			txBuilder.verify(getServiceHub());

			progressTracker.setCurrentStep(SIGNING_TRANSACTION);
			// Sign the transaction.
			final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

			progressTracker.setCurrentStep(GATHERING_SIGS);
			// Send the state to the counterparty, and receive it back with their signature.
			FlowSession otherPartySession = initiateFlow(otherParty);
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
						require.using("The output dcr state requires a owner.", dcr.getOwner() != null);
						require.using("The output dcr state requires a trade id.", dcr.getTradeId() != null);
						Party me = getOurIdentity();
						boolean isForMe = dcr.getOwner().getOwningKey().equals(me.getOwningKey());
						require.using("The flow can only be run by the owner", isForMe);
						require.using("The output state are not valid, expected dcr state status [EARMARKED] but found [" + dcr.getStatus() + "]", dcr.getStatus().equals("EARMARKED"));
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
