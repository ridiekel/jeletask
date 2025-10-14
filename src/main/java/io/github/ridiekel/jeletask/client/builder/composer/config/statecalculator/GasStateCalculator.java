package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.GasState;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class GasStateCalculator extends StateCalculatorSupport<GasState> {
    @Override
    public GasState fromEvent(ComponentSpec component, byte[] dataBytes) {
        // Default value
        float gas_value = NumberConverter.UNSIGNED_SHORT.convert(dataBytes).longValue();

        // Min and Max values are configurable. Use the same values you already use in PROSOFT!
        float mMax = component.getGas_max();
        float mMin = component.getGas_min();

        // There are 3 different General Analog Sensors in Teletask.
        gas_value = switch (component.getGas_type().toLowerCase()) {
            case "4-20ma" -> (mMax - mMin) / 704.0f * (gas_value - 176) + mMin;
            case "0-10v", "5-10v" -> (mMax - mMin) / 1023.0f * gas_value + mMin;
            case "0-20ma" -> (mMax - mMin) / 880.0f * gas_value + mMin;
            default -> gas_value;
        };

        // Round up to X decimals
        BigDecimal gas_rounded_value = new BigDecimal(gas_value);
        gas_rounded_value = gas_rounded_value.setScale(component.getDecimals(), RoundingMode.HALF_UP);

        return new GasState(gas_rounded_value);
    }

    @Override
    public byte[] toCommand(GasState state) {
        throw new IllegalArgumentException("Gas state is read only. Strange that we get in the serialize method");
    }

    @Override
    protected Class<GasState> getStateType() {
        return GasState.class;
    }
}
