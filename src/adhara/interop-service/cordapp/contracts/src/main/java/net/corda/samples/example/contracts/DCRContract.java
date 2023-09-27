package net.corda.samples.example.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.example.states.DCRState;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class DCRContract implements Contract {
    public static final String ID = "net.corda.samples.example.contracts.DCRContract";

    @Override
    public void verify(LedgerTransaction tx) {
        List<CommandWithParties<CommandData>> dcrCommands = tx.getCommands().stream().filter(e -> e.getValue() instanceof Commands).collect(Collectors.toList());
        requireThat(require -> {
            require.using("There should only be one dcr command present.",dcrCommands.size() < 2);
            return null;
        });
        for (final CommandWithParties<CommandData> command : dcrCommands) {
            final Commands commandData = (Commands) command.getValue();
            if (commandData.equals(new Commands.Create())) {
                requireThat(require -> {
                    require.using("No inputs should be present when issuing.", tx.getInputs().isEmpty());
                    require.using("Only one output states should be created when issuing.", tx.getOutputs().size() == 1);
                    final DCRState out = tx.outputsOfType(DCRState.class).get(0);
                    require.using("The issuer and the owner must be the same entity.", out.getIssuer().equals(out.getOwner()));
                    require.using("The issuer must be a signer.", command.getSigners().contains(out.getIssuer().getOwningKey()));
                    require.using("The value must be non-negative.", new BigInteger(out.getValue(), 10).signum() > 0);
                    require.using("The currency must not be null.", !out.getCurrency().isEmpty());
                    return null;
                });
            } else if (commandData.equals(new Commands.Earmark())) {
                requireThat(require -> {
                    require.using("There should be inputs when earmarking.", !tx.getInputs().isEmpty());
                    require.using("Only one output states should be created when earmarking.", tx.getOutputs().size() == 1);
                    final DCRState out = tx.outputsOfType(DCRState.class).get(0);
                    require.using("The earmark needs to specify a new owner.", out.getOwner() != null);
                    require.using("The issuer and the owner cannot be the same entity.", !out.getOwner().equals(out.getIssuer()));
                    require.using("All of the participants must be signers.", command.getSigners().containsAll(out.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));
                    require.using("The trade id should be present.", out.getTradeId() != null);
                    require.using("The proof must be cleared.", out.getProof() == null);
                    return null;
                });
            } else if (commandData.equals(new Commands.Confirm())) {
                requireThat(require -> {
                    require.using("There should be inputs when confirming.", !tx.getInputs().isEmpty());
                    require.using("Only one output states should be created when confirming.", tx.getOutputs().size() == 1);
                    final DCRState out = tx.outputsOfType(DCRState.class).get(0);
                    require.using("All of the participants must be signers.", command.getSigners().containsAll(out.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));
                    require.using("The proof must be present.", out.getProof() != null && !out.getProof().isEmpty());
                    return null;
                });
            } else if (commandData.equals(new Commands.Cancel())) {
                requireThat(require -> {
                    // Generic constraints around the DCR transaction.
                    require.using("There should be inputs when cancelling.", !tx.getInputs().isEmpty());
                    require.using("Only one output states should be created when cancelling.", tx.getOutputs().size() == 1);
                    final DCRState out = tx.outputsOfType(DCRState.class).get(0);
                    require.using("All of the participants must be signers.", command.getSigners().containsAll(out.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));
                    require.using("The proof must be present.", out.getProof() != null && !out.getProof().isEmpty());
                    return null;
                });
            }
        }
    }

    public interface Commands extends CommandData {
        class Create implements Commands {}
        class Earmark implements Commands {}
        class Confirm implements Commands {}
        class Cancel implements Commands {}
    }
}
