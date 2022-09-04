package io.github.ridiekel.jeletask.client.builder.composer.config.configurables;

import io.github.ridiekel.jeletask.client.builder.composer.config.Configurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.StateCalculator;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

public class FunctionConfigurable extends Configurable<Function> {
    private final StateCalculator stateCalculator;

    public FunctionConfigurable(Function function, int number, StateCalculator stateCalculator) {
        super(number, function);
        this.stateCalculator = stateCalculator;
    }

    public StateCalculator getStateCalculator(ComponentSpec componentSpec) {
        return this.stateCalculator.forComponent(componentSpec);
    }

    public boolean isValidState(ComponentSpec component, ComponentState state) {
        return this.getStateCalculator(component).isValidState(state);
    }
}
