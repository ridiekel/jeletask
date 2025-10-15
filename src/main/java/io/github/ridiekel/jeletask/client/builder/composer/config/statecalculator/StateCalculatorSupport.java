package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.State;

import static io.github.ridiekel.jeletask.client.spec.state.State.OBJECT_MAPPER;

public abstract class StateCalculatorSupport<S extends State<?>> implements StateCalculator<S> {
    protected StateCalculatorSupport() {
    }

    @Override
    public S stateFromMessage(String message) {
        S stateObject = null;
        try {
            stateObject = OBJECT_MAPPER.readValue(message, getStateType());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not parse JSON message to correct state type (" + getStateType() + "): " + message, e);
        }

        return stateObject;
    }

    @Override
    public byte[] toEventForTesting(S state) {
        return toCommand(state);
    }

    @Override
    public S fromCommandForTesting(ComponentSpec component, byte[] dataBytes) {
        return fromEvent(component, dataBytes);
    }

    protected abstract Class<S> getStateType();

    @Override
    public boolean isValidWriteState(S state) {
        return state != null;
    }
}
