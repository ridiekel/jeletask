package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.TemperatureState;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TemperatureStateCalculator extends StateCalculatorSupport<TemperatureState> {
    public static final NumberConverter NUMBER_CONVERTER = NumberConverter.UNSIGNED_SHORT;
    private static final BigDecimal _273 = new BigDecimal("273");

    @Override
    protected Class<TemperatureState> getStateType() {
        return TemperatureState.class;
    }

    @Override
    public TemperatureState fromEvent(ComponentSpec component, byte[] dataBytes) {
        BigDecimal rounded_value = BigDecimal.valueOf(NUMBER_CONVERTER.convert(dataBytes).longValue())
                .divide(BigDecimal.TEN, component.getDecimals(), RoundingMode.HALF_UP)
                .subtract(_273);

        return new TemperatureState(rounded_value);
    }

    @Override
    public byte[] toCommand(TemperatureState value) {
        BigDecimal multiplied = value.getState().add(_273).multiply(BigDecimal.TEN);

        return NUMBER_CONVERTER.convert(multiplied.longValue());
    }
}
