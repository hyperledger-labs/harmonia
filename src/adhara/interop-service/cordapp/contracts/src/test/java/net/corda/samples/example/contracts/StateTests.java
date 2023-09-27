package net.corda.samples.example.contracts;

import net.corda.samples.example.states.DCRState;
import net.corda.testing.node.MockServices;
import org.junit.Test;

public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    @Test
    public void hasAmountFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        DCRState.class.getDeclaredField("value");
        // Is the message field of the correct type?
        assert(DCRState.class.getDeclaredField("value").getType().equals(Integer.class));
    }
}