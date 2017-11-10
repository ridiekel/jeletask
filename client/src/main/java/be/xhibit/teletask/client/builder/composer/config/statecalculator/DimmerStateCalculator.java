package be.xhibit.teletask.client.builder.composer.config.statecalculator;

import be.xhibit.teletask.client.builder.composer.config.NumberConverter;
import be.xhibit.teletask.model.spec.ComponentSpec;

public class DimmerStateCalculator extends PercentageStateCalculator {
    public DimmerStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }

    @Override
    public String getDefaultState(ComponentSpec component) {
        return "50";
    }
}