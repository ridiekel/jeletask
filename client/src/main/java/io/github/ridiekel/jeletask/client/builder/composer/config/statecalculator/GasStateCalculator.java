package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class GasStateCalculator extends SimpleStateCalculator {
    public GasStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }
    @Override
    public ComponentState toComponentState(ComponentSpec component, byte[] dataBytes) {
        long longValue = this.getNumberConverter().convert(dataBytes).longValue();

        // Default in case gas_type was not defined.
        float gas_value = longValue;

        // Min and Max values are configurable. Use the same values you already use in PROSOFT
        float mMax = component.getGas_max();
        float mMin = component.getGas_min();

        // There are 3 different General Analog Sensors in Teletask.
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

        // Round up to X decimals
        BigDecimal gas_rounded_value = new BigDecimal(gas_value);
        gas_rounded_value = gas_rounded_value.setScale(component.getGas_decimals(), RoundingMode.HALF_UP);

        return new ComponentState(gas_rounded_value.toString());
    }
}
