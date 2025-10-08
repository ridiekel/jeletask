package io.github.ridiekel.jeletask.client.builder.composer.config.configurables;

import io.github.ridiekel.jeletask.client.builder.composer.config.Configurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.StateCalculator;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;

public class FunctionConfigurable extends Configurable<Function> {
    private final java.util.function.Function<ComponentSpec, StateCalculator<?>> stateCalculator;

    public FunctionConfigurable(Function function, int number, StateCalculator<?> stateCalculator) {
        this(function, number, c -> stateCalculator);
    }

    public FunctionConfigurable(Function function, int number, java.util.function.Function<ComponentSpec, StateCalculator<?>> stateCalculator) {
        super(number, function);
        this.stateCalculator = stateCalculator;
    }

    @SuppressWarnings("unchecked")
    public <C extends StateCalculator<?>> C getStateCalculator(ComponentSpec componentSpec) {
        return (C) this.stateCalculator.apply(componentSpec);
    }
}
