package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.model.spec.ComponentSpec;

public class HumidityStateCalculator extends PercentageStateCalculator {
    public HumidityStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }

    @Override
    public String getDefaultState(ComponentSpec component) {
        return "25";
    }
}