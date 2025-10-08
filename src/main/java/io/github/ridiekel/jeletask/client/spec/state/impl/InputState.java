package io.github.ridiekel.jeletask.client.spec.state.impl;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.InputStateCalculator;
import io.github.ridiekel.jeletask.client.spec.state.State;

public class InputState extends State<InputStateCalculator.ValidInputState> {
    public InputState() {
    }

    public InputState(InputStateCalculator.ValidInputState state) {
        super(state);
    }
}
