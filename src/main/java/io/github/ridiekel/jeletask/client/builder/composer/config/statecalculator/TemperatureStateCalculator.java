package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.TemperatureState;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * REPORTS 2 bytes:
 * To change from short to °C: short/10 -273
 * To change from °C to short: °C+273 *10
 * <p>
 * Note: this formula applies for actual states, for temperature differences (the STANDBY value)
 * you only need to multiply or divide by 10 (e.g. ± 3.5°C = 35)
 * <p>
 * SETS 1 byte:
 * To change from byte to °C: (byte/2)-40
 * To change from °C to byte: (temp+40)x2
 */
public class TemperatureStateCalculator extends StateCalculatorSupport<TemperatureState> {
    public static final NumberConverter NUMBER_CONVERTER = NumberConverter.UNSIGNED_SHORT;
    private static final BigDecimal _273 = new BigDecimal("273");

    @Override
    protected Class<TemperatureState> getStateType() {
        return TemperatureState.class;
    }

    public static BigDecimal bytesToBigDecimal(ComponentSpec component, byte[] dataBytes) {
        return BigDecimal.valueOf(NUMBER_CONVERTER.convert(dataBytes).longValue())
                .divide(BigDecimal.TEN, component.getDecimals(), RoundingMode.HALF_UP)
                .subtract(_273);
    }

    public static byte[] bigDecimalToBytes(BigDecimal value) {
        return NUMBER_CONVERTER.convert(value.add(_273).multiply(BigDecimal.TEN).longValue());
    }

    @Override
    public TemperatureState fromEvent(ComponentSpec component, byte[] dataBytes) {
        BigDecimal rounded_value = bytesToBigDecimal(component, dataBytes);

        return new TemperatureState(rounded_value);
    }

    @Override
    public byte[] toCommand(TemperatureState state) {
        return bigDecimalToBytes(state.getState());
    }
}
