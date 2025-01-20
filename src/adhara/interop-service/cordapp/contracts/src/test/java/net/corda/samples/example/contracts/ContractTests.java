package net.corda.samples.example.contracts;

import net.corda.samples.example.states.DCRState;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class ContractTests {
    static private final MockServices ledgerServices = new MockServices();
    static private final TestIdentity megaCorp = new TestIdentity(new CordaX500Name("MegaCorp", "London", "GB"));
    static private final TestIdentity miniCorp = new TestIdentity(new CordaX500Name("MiniCorp", "London", "GB"));
    static private final String dcrValue = "1";
    static private final String dcrCurrency = "GBP";
    static private final String networkId = "0";
    static private final String contractAddress = "0x123";
    static private final String receiverId = "0x456";
    static private final String senderId = "0x789";
    static private final String tokenAmount = "1";
    static private final String tradeId = "123";

    @Test
    public void transactionMustIncludeCreateCommand() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(DCRContract.ID, new DCRState(dcrValue, dcrCurrency, megaCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier(),  tradeId, null, "AVAILABLE"));
                tx.fails();
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new DCRContract.Commands.Create());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionMustHaveNoInputs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(DCRContract.ID, new DCRState(dcrValue, dcrCurrency, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier(),  tradeId, null, "AVAILABLE"));
                tx.output(DCRContract.ID, new DCRState(dcrValue, dcrCurrency, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier(),  tradeId, null, "AVAILABLE"));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new DCRContract.Commands.Create());
                tx.failsWith("No inputs should be present when issuing.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionMustHaveOneOutput() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(DCRContract.ID, new DCRState(dcrValue, dcrCurrency, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier(),  tradeId,null, "AVAILABLE"));
                tx.output(DCRContract.ID, new DCRState(dcrValue, dcrCurrency, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier(),  tradeId,null, "AVAILABLE"));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new DCRContract.Commands.Create());
                tx.failsWith("Only one output states should be created when issuing.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void issuerMustSignTransaction() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(DCRContract.ID, new DCRState(dcrValue, dcrCurrency, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier(),  tradeId,null, "AVAILABLE"));
                tx.command(miniCorp.getPublicKey(), new DCRContract.Commands.Create());
                tx.failsWith("The issuer must be a signer.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void ownerMustSignTransaction() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(DCRContract.ID, new DCRState(dcrValue, dcrCurrency, miniCorp.getParty(), miniCorp.getParty(), new UniqueIdentifier(),  tradeId,null, "AVAILABLE"));
                tx.command(megaCorp.getPublicKey(), new DCRContract.Commands.Create());
                tx.failsWith("The issuer must be a signer.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void cannotCreateNegativeValueDCRs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(DCRContract.ID, new DCRState("-1", dcrCurrency, miniCorp.getParty(), megaCorp.getParty(), new UniqueIdentifier(), tradeId,null, "AVAILABLE"));
                tx.command(ImmutableList.of(megaCorp.getPublicKey(), miniCorp.getPublicKey()), new DCRContract.Commands.Create());
                tx.failsWith("The value must be non-negative.");
                return null;
            });
            return null;
        }));
    }
}
