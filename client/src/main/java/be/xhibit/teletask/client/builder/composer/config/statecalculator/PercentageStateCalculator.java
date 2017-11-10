package be.xhibit.teletask.client.builder.composer.config.statecalculator;

import be.xhibit.teletask.client.builder.composer.config.NumberConverter;

public class PercentageStateCalculator extends SimpleStateCalculator {
    public PercentageStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }

    @Override
    public boolean isValidState(String state) {
        Long value = Long.valueOf(state);
        return value >= 0 && value <= 100;
    }
}