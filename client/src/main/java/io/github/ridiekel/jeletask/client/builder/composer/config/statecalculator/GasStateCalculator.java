package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;

public class GasStateCalculator extends SimpleStateCalculator {
    public GasStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }
    
    @Override
    public ComponentState toComponentState(ComponentSpec component, byte[] dataBytes) {
        long longValue = this.getNumberConverter().convert(dataBytes).longValue();
        float mMax = component.getGas_max();
        float mMin = component.getGas_min();
        float gas_value = 0;
        switch (component.getGas_type()) {
            case "4-20ma":
                gas_value = (mMax - mMin) / 704.0f * (longValue - 176) + mMin;
                break;
            case "0-10v":
            case "5-10v":
                gas_value = (mMax - mMin) / 1023.0f * longValue + mMin;
                break;
            case "0-20ma":
                gas_value = (mMax - mMin) / 880.0f * longValue + mMin;
                break;
        }

        return new ComponentState(Math.round(gas_value));
    }
}
