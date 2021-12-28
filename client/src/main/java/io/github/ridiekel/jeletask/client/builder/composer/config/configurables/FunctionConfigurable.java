package io.github.ridiekel.jeletask.client.builder.composer.config.configurables;

import io.github.ridiekel.jeletask.client.builder.composer.config.Configurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.StateCalculator;
import io.github.ridiekel.jeletask.client.spec.Function;

public class FunctionConfigurable extends Configurable<Function> {
    private final StateCalculator stateCalculator;

    public FunctionConfigurable(Function function, int number, StateCalculator stateCalculator) {
        super(number, function);
        this.stateCalculator = stateCalculator;
    }

    public StateCalculator getStateCalculator() {
        return this.stateCalculator;
    }

    public boolean isValidState(String state) {
        return this.getStateCalculator().isValidState(state);
    }
}
