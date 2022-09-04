package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

public class PercentageStateCalculator extends SimpleStateCalculator {
    public PercentageStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }

    @Override
    public boolean isValidState(ComponentState state) {
        long value = Long.parseLong(state.getState());
        return value >= 0 && value <= 100;
    }
}
