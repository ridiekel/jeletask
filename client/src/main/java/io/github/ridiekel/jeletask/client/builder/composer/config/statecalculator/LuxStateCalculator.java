package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;

public class LuxStateCalculator extends SimpleStateCalculator {
    public LuxStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }

    @Override
    public String convertGet(ComponentSpec component, byte[] value) {
        long longValue = this.getNumberConverter().convert(value).longValue();
        double exponent = longValue / 40d;
        double powered = Math.pow(10, exponent);
        double luxValue = powered - 1;
        return String.valueOf(Math.round(luxValue));
    }

    @Override
    public byte[] convertSet(ComponentSpec component, String value) {
        Long longValue = Long.valueOf(value);
        long inBetween = longValue + 1;
        double log10 = Math.log10(inBetween);
        double convertedValue = log10 * 40;
        return this.getNumberConverter().convert(Math.round(convertedValue));
    }

    @Override
    public String getDefaultState(ComponentSpec component) {
        return "3547";
    }
}
