package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

public class DimmerStateCalculator extends SimpleStateCalculator {
    public DimmerStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }

    @Override
    public ComponentState getDefaultState(ComponentSpec component) {
        return new ComponentState("50");
    }

    @Override
    public boolean isValidState(ComponentState state) {
        long value = Long.parseLong(state.getState());
        return value == 103 || (value >= 0 && value <= 100);
    }
}

