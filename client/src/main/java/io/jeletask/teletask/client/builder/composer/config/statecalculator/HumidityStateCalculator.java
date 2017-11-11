package io.jeletask.teletask.client.builder.composer.config.statecalculator;

import io.jeletask.teletask.client.builder.composer.config.NumberConverter;
import io.jeletask.teletask.model.spec.ComponentSpec;

public class HumidityStateCalculator extends PercentageStateCalculator {
    public HumidityStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }

    @Override
    public String getDefaultState(ComponentSpec component) {
        return "25";
    }
}