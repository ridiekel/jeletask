package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.GasState;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.function.TriFunction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * With Smax en Smin being the minimum and maximum values for “value”, the value can be calculated from the short as:
 * <p>
 * 4-20mA:
 * - Value = ( ( ( Smax – Smin ) / 720 ) x ( short - 180) ) + Smin
 * <p>
 * 0-20mA:
 * Value = ( ( ( Smax – Smin ) / 900 ) x value ) + Smin
 * <p>
 * 0-10V and 5-10V:
 * Value = ( ( ( Smax - Smin) / 1023) x value ) + Smin
 */
public class GasStateCalculator extends StateCalculatorSupport<GasState> {
    private static final Map<String, GasTypeConfig> CONFIGS = Stream.of(
            new GasTypeConfig(
                    "4-20ma",
                    (value, min, max) -> {
                        return max.subtract(min)
                                .divide(new BigDecimal("704.0"), 10, RoundingMode.HALF_UP)
                                .multiply(value.subtract(new BigDecimal("176")))
                                .add(min);
                    },
                    (value, min, max) -> {
                        return max;
                    }
            ),
            new GasTypeConfig(
                    "0-10v",
                    (value, min, max) -> {
                        return max.subtract(min)
                                .divide(new BigDecimal("1023.0"), 10, RoundingMode.HALF_UP)
                                .multiply(value)
                                .add(min);
                    },
                    (value, min, max) -> {
                        return max;
                    }
            ),
            new GasTypeConfig(
                    "5-10v",
                    (value, min, max) -> {
                        return max.subtract(min)
                                .divide(new BigDecimal("1023.0"), 10, RoundingMode.HALF_UP)
                                .multiply(value)
                                .add(min);
                    },
                    (value, min, max) -> {
                        return max;
                    }
            ),
            new GasTypeConfig(
                    "0-20ma",
                    (value, min, max) -> {
                        return max.subtract(min)
                                .divide(new BigDecimal("880.0"), 10, RoundingMode.HALF_UP)
                                .multiply(value)
                                .add(min);
                    },
                    (value, min, max) -> {
                        return max;
                    }
            )
    ).collect(Collectors.toMap(t -> t.name, Function.identity()));

    @Override
    public GasState fromEvent(ComponentSpec component, byte[] dataBytes) {
        // Default value
        float gas_value = NumberConverter.UNSIGNED_SHORT.convert(dataBytes).longValue();

        // Min and Max values are configurable. Use the same values you already use in PROSOFT!
        float mMax = component.getGas_max();
        float mMin = component.getGas_min();

        // There are 3 different General Analog Sensors in Teletask.
        gas_value = switch (component.getGas_type().toLowerCase()) {
            case "4-20ma" -> fromOne(gas_value, mMin, mMax);
            case "0-10v", "5-10v" -> fromTwo(gas_value, mMin, mMax);
            case "0-20ma" -> (mMax - mMin) / 880.0f * gas_value + mMin;
            default -> gas_value;
        };

        // Round up to X decimals
        BigDecimal gas_rounded_value = new BigDecimal(gas_value);
        gas_rounded_value = gas_rounded_value.setScale(component.getDecimals(), RoundingMode.HALF_UP);

        return new GasState(gas_rounded_value);
    }

    private static float fromThree(float gas_value, float mMin, float mMax) {
        return (mMax - mMin) / 880.0f * gas_value + mMin;
    }

    private static float fromTwo(float gas_value, float mMin, float mMax) {
        return (mMax - mMin) / 1023.0f * gas_value + mMin;
    }

    private static float fromOne(float gas_value, float mMin, float mMax) {
        return (mMax - mMin) / 704.0f * (gas_value - 176) + mMin;
    }

    @Override
    public byte[] toCommand(GasState state) {
        return new byte[]{0, 0};
    }

    @Override
    public GasState fromCommandForTesting(ComponentSpec component, byte[] dataBytes) {
        return super.fromCommandForTesting(component, dataBytes);
    }

    @Override
    protected Class<GasState> getStateType() {
        return GasState.class;
    }

    @RequiredArgsConstructor
    private static final class GasTypeConfig {

        private final String name;
        private final TriFunction<BigDecimal, BigDecimal, BigDecimal, BigDecimal> fromEvent;
        private final TriFunction<BigDecimal, BigDecimal, BigDecimal, BigDecimal> toCommand;
    }

}
