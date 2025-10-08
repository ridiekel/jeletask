package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.State;

public interface StateCalculator<S extends State<?>> {
    S fromEvent(ComponentSpec component, byte[] dataBytes);

    /**
     * Only used during testing. I added this here for convenience.
     * <p>
     * This should return the State representation of the command as the Teletask server would need to interpret as a command is received by the test server.
     * This is used by the test server to convert a byte array to a command state. The test server will match the returned state based on the mocking config.
     *
     * @param component The component this command is tied to.
     * @param dataBytes The bytes that the test server received.
     * @return The State representation of the byte array.
     */
    S fromCommandForTesting(ComponentSpec component, byte[] dataBytes);

    S stateFromMessage(String message);

    byte[] toCommand(S state);

    /**
     * Only used during testing. I added this here for convenience.
     *
     * This should return the byte[] representation of the state as the Teletask server would do.
     * This is used by the test server to convert a state to an event byte array. The test server will return this based on the mocking config.
     *
     * @param state The state to convert.
     * @return The byte array representation of the event.
     */
    byte[] toEventForTesting(S state);

    boolean isValidWriteState(S state);
}
