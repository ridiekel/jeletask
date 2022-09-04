package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

public class LuxStateCalculator extends SimpleStateCalculator {
    public LuxStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }

    @Override
    public ComponentState convertGet(byte[] dataBytes) {
        long longValue = this.getNumberConverter().convert(dataBytes).longValue();
        double exponent = longValue / 40d;
        double powered = Math.pow(10, exponent);
        double luxValue = powered - 1;
        return new ComponentState(Math.round(luxValue));
    }

    @Override
    public byte[] convertSetState(ComponentState value) {
        long longValue = Long.parseLong(value.getState());
        long inBetween = longValue + 1;
        double log10 = Math.log10(inBetween);
        double convertedValue = log10 * 40;
        return this.getNumberConverter().convert(Math.round(convertedValue));
    }

    @Override
    public ComponentState getDefaultState(ComponentSpec component) {
        return new ComponentState("3547");
    }
}
