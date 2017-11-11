package io.jeletask.client.builder.composer.config.statecalculator;

import io.jeletask.client.builder.composer.config.NumberConverter;
import io.jeletask.model.spec.ComponentSpec;

public class DimmerStateCalculator extends PercentageStateCalculator {
    public DimmerStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }

    @Override
    public String getDefaultState(ComponentSpec component) {
        return "50";
    }
}