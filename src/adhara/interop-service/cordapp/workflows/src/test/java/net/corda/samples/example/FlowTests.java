package net.corda.samples.example;

import net.corda.core.identity.CordaX500Name;
import net.corda.samples.example.flows.CreateDCRFlow;
import net.corda.samples.example.flows.CreateXVPFlow;
import net.corda.samples.example.states.DCRState;
import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;

    @Before
    public void setup() {

        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.example.contracts"),
                TestCordapp.findCordapp("net.corda.samples.example.flows")))
                .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB"))))
        );
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }


    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void flowRejectsInvalidDCRs() throws Exception {
        // The DCRContract specifies that DCRs cannot have negative values.
        CreateDCRFlow.Initiator flow = new CreateDCRFlow.Initiator("-1", "GBP");
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();

        // The DCRContract specifies that DCRs cannot have negative values.
        exception.expectCause(instanceOf(TransactionVerificationException.class));
        future.get();
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        CreateDCRFlow.Initiator flow = new CreateDCRFlow.Initiator("1", "GBP");
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(b.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        CreateDCRFlow.Initiator flow = new CreateDCRFlow.Initiator("1", "GBP");
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(a.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        CreateDCRFlow.Initiator flow = new CreateDCRFlow.Initiator("1", "GBP");
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void recordedTransactionHasNoInputsAndASingleOutputTheInputDCR() throws Exception {
        CreateDCRFlow.Initiator flow = new CreateDCRFlow.Initiator("1", "GBP");
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(signedTx.getId());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assert (txOutputs.size() == 1);

            DCRState recordedState = (DCRState) txOutputs.get(0).getData();
            assertEquals(recordedState.getValue(), 1);
            assertEquals(recordedState.getOwner(), a.getInfo().getLegalIdentities().get(0));
            assertEquals(recordedState.getIssuer(), b.getInfo().getLegalIdentities().get(0));
        }
    }

    @Test
    public void flowRecordsTheCorrectDCRInBothPartiesVaults() throws Exception {
        Integer iouValue = 1;
        CreateDCRFlow.Initiator flow = new CreateDCRFlow.Initiator("1", "GBP");
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        future.get();

        // We check the recorded DCR in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            node.transaction(() -> {
                List<StateAndRef<DCRState>> dcrs = node.getServices().getVaultService().queryBy(DCRState.class).getStates();
                assertEquals(1, dcrs.size());
                DCRState recordedState = dcrs.get(0).getState().getData();
                assertEquals(recordedState.getValue(), iouValue);
                assertEquals(recordedState.getOwner(), a.getInfo().getLegalIdentities().get(0));
                assertEquals(recordedState.getIssuer(), b.getInfo().getLegalIdentities().get(0));
                return null;
            });
        }
    }

    @Test
    public void flowTransactionInBothPartiesTransactionStorages() throws Exception {
        CreateXVPFlow.Initiator flow = new CreateXVPFlow.Initiator("123", "000", a.getInfo().getLegalIdentities().get(0), b.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }
}
