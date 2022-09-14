package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;

public class GasStateCalculator extends SimpleStateCalculator {
    public GasStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }

    public String convertGet(ComponentSpec component, byte[] value) {
        short base = this.getNumberConverter().convert(value).shortValue();
        float mMax = component.getGas_max();
        float mMin = component.getGas_min();
        float gas_value = 0;
        switch (component.getGas_type()) {
            case "4-20ma":
                gas_value = (mMax - mMin) / 704.0f * (base - 176) + mMin;
                break;
            case "0-10v":
            case "5-10v":
                gas_value = (mMax - mMin) / 1023.0f * base + mMin;
                break;
            case "0-20ma":
                gas_value = (mMax - mMin) / 880.0f * base + mMin;
                break;
        }

        return String.valueOf(gas_value);
    }
}
