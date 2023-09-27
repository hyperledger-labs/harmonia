package net.corda.samples.example.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import net.corda.samples.example.contracts.DCRContract;
import net.corda.samples.example.states.DCRState;
import java.util.ArrayList;
import java.util.Arrays;

public class CreateDCRFlow {
	@InitiatingFlow
	@StartableByRPC
	public static class Initiator extends FlowLogic<SignedTransaction> {

		private final String dcrValue;
		private final String dcrCurrency;

		private final Step GENERATING_TRANSACTION = new Step("Generating transaction to issue an new DCR.");
		private final Step VERIFYING_TRANSACTION = new Step("Verifying contract constraints.");
		private final Step SIGNING_TRANSACTION = new Step("Signing transaction with our private key.");
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
			FINALISING_TRANSACTION
		);

		public Initiator(String dcrValue, String dcrCurrency) {
			this.dcrValue = dcrValue;
			this.dcrCurrency = dcrCurrency;
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

			progressTracker.setCurrentStep(GENERATING_TRANSACTION);
			// Generate an unsigned transaction.
			Party me = getOurIdentity();
			DCRState dcrState = new DCRState(dcrValue, dcrCurrency, me, me, new UniqueIdentifier(), null, null, "AVAILABLE");
			final Command<DCRContract.Commands.Create> txCommand = new Command<>(
				new DCRContract.Commands.Create(),
				Arrays.asList(dcrState.getIssuer().getOwningKey()));
			final TransactionBuilder txBuilder = new TransactionBuilder(notary)
				.addOutputState(dcrState, DCRContract.ID)
				.addCommand(txCommand);

			progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
			// Verify that the transaction is valid.
			txBuilder.verify(getServiceHub());

			progressTracker.setCurrentStep(SIGNING_TRANSACTION);
			// Sign the transaction.
			final SignedTransaction fullySignedTx = getServiceHub().signInitialTransaction(txBuilder);

			progressTracker.setCurrentStep(FINALISING_TRANSACTION);
			// Notarise and record the transaction in both parties' vaults.
			return subFlow(new FinalityFlow(fullySignedTx, new ArrayList<>(), StatesToRecord.ALL_VISIBLE));
		}
	}
}
