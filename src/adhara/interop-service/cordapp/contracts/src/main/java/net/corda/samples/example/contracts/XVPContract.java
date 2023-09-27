package net.corda.samples.example.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.example.states.XVPState;

import java.util.List;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class XVPContract implements Contract {
    public static final String ID = "net.corda.samples.example.contracts.XVPContract";

    @Override
    public void verify(LedgerTransaction tx) {
        List<CommandWithParties<CommandData>> xvpCommands = tx.getCommands().stream().filter(e -> e.getValue() instanceof Commands).collect(Collectors.toList());
        requireThat(require -> {
            require.using("There should only be one xvp command present.",xvpCommands.size() < 2);
            return null;
        });

        for (final CommandWithParties<CommandData> command : xvpCommands) {
            final Commands commandData = (Commands) command.getValue();
            if (commandData.equals(new Commands.Create())) {
                requireThat(require -> {
                    // Generic constraints around the XvP trade creation transaction.
                    require.using("No inputs should be present when creating a xvp trade.", tx.getInputs().isEmpty());
                    require.using("Only one output states should be created.", tx.getOutputs().size() == 1);
                    final XVPState out = tx.outputsOfType(XVPState.class).get(0);
                    require.using("All of the participants must be signers.", command.getSigners().containsAll(out.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));
                    return null;
                });
            } else if (commandData.equals(new Commands.Cancel())) {
                requireThat(require -> {
                    // Generic constraints around the XvP trade cancellation transaction.
                    require.using("There should be inputs when cancelling.", !tx.getInputs().isEmpty());
                    require.using("Only one output states should be created.", tx.getOutputs().size() == 1);
                    final XVPState out = tx.outputsOfType(XVPState.class).get(0);
                    require.using("All of the participants must be signers.", command.getSigners().containsAll(out.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));
                    return null;
                });
            } else if (commandData.equals(new Commands.Resolve())) {
                requireThat(require -> {
                    // Generic constraints around the XvP trade resolution transaction.
                    require.using("There should be inputs when resolving.", !tx.getInputs().isEmpty());
                    require.using("No output states should be present when resolving.", tx.getOutputs().isEmpty());
                    final XVPState in = tx.inputsOfType(XVPState.class).get(0);
                    require.using("All of the participants must be signers.", command.getSigners().containsAll(in.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));
                    return null;
                });
            }
        }
    }

    public interface Commands extends CommandData {
        class Create implements Commands {}
        class Cancel implements Commands {}
        class Resolve implements Commands {}
    }
}