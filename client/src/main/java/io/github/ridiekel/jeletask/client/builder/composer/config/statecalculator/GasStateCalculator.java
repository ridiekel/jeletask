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
        // Default value
        float gas_value = this.getNumberConverter().convert(dataBytes).longValue();

        // Min and Max values are configurable. Use the same values you already use in PROSOFT!
        float mMax = component.getGas_max();
        float mMin = component.getGas_min();

        // There are 3 different General Analog Sensors in Teletask.
        switch (component.getGas_type().toLowerCase()) {
            case "4-20ma":
                gas_value = (mMax - mMin) / 704.0f * (gas_value - 176) + mMin;
                break;
            case "0-10v":
            case "5-10v":
                gas_value = (mMax - mMin) / 1023.0f * gas_value + mMin;
                break;
            case "0-20ma":
                gas_value = (mMax - mMin) / 880.0f * gas_value + mMin;
                break;
        }

        // Round up to X decimals
        BigDecimal gas_rounded_value = new BigDecimal(gas_value);
        gas_rounded_value = gas_rounded_value.setScale(component.getDecimals(), RoundingMode.HALF_UP);

        return new ComponentState(gas_rounded_value.toString());
    }
}
