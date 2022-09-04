package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

public interface StateCalculator {
    ComponentState convertGet(byte[] dataBytes);

    byte[] convertSetState(ComponentState state);

    NumberConverter getNumberConverter();

    boolean isValidState(ComponentState state);

    ComponentState getDefaultState(ComponentSpec component);

    default StateCalculator forComponent(ComponentSpec component) {
        return this;
    }
}
