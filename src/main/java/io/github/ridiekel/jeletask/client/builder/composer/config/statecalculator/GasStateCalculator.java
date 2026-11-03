package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.component.GasSensorSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.GasState;
import org.apache.commons.lang3.function.TriFunction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
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
    @Override
    public GasState fromEvent(ComponentSpec component, byte[] dataBytes) {
        return new GasState(fromEventValue(component, new BigDecimal(NumberConverter.UNSIGNED_SHORT.convert(dataBytes).longValue())));
    }

    public static BigDecimal fromEventValue(ComponentSpec component, BigDecimal value) {
        GasSensorSpec gas = (GasSensorSpec) component;
        BigDecimal min = Optional.ofNullable(gas.getGasMin()).orElseThrow(() -> new IllegalArgumentException("Gas min cannot be null"));
        BigDecimal max = Optional.ofNullable(gas.getGasMax()).orElseThrow(() -> new IllegalArgumentException("Gas max cannot be null"));

        BigDecimal calculated = Optional.ofNullable(gas.getGasType()).map(CONFIGS::get).map(c -> c.fromEvent.apply(value, min, max)).orElseThrow(() -> new IllegalArgumentException("Gas type '" + gas.getGasType() + "' not found in: " + CONFIGS));

        return calculated.setScale(Optional.ofNullable(gas.getDecimals()).orElse(2), RoundingMode.HALF_UP);
    }

    @Override
    public byte[] toCommand(ComponentSpec component, GasState state) {
        BigDecimal value = state.getState();

        BigDecimal calculated = toCommandValue(component, value);

        return NumberConverter.UNSIGNED_SHORT.convert(calculated);
    }

    public static BigDecimal toCommandValue(ComponentSpec component, BigDecimal value) {
        GasSensorSpec gas = (GasSensorSpec) component;
        BigDecimal min = Optional.ofNullable(gas.getGasMin()).orElseThrow(() -> new IllegalArgumentException("Gas min cannot be null"));
        BigDecimal max = Optional.ofNullable(gas.getGasMax()).orElseThrow(() -> new IllegalArgumentException("Gas max cannot be null"));

        return Optional.ofNullable(gas.getGasType()).map(CONFIGS::get).map(c -> c.toCommand.apply(value, min, max)).orElseThrow(() -> new IllegalArgumentException("Gas type '" + gas.getGasType() + "' not found in: " + CONFIGS));
    }

    @Override
    protected Class<GasState> getStateType() {
        return GasState.class;
    }

    // Raw analog input values reported by the central unit for the supported sensor signal ranges
    private static final String RAW_SPAN_4_20MA = "704.0";
    private static final String RAW_OFFSET_4MA = "176";
    private static final String RAW_FULL_SCALE_10BIT = "1023.0";
    private static final String RAW_FULL_SCALE_0_20MA = "880.0";

    private static final Map<String, GasTypeConfig> CONFIGS = Stream.of(
            new GasTypeConfig("4-20ma",
                    (value, min, max) -> max.subtract(min)
                            .divide(new BigDecimal(RAW_SPAN_4_20MA), 10, RoundingMode.HALF_UP)
                            .multiply(value.subtract(new BigDecimal(RAW_OFFSET_4MA)))
                            .add(min),
                    (value, min, max) -> value.subtract(min)
                            .multiply(new BigDecimal(RAW_SPAN_4_20MA))
                            .divide(max.subtract(min), 10, RoundingMode.HALF_UP)
                            .add(new BigDecimal(RAW_OFFSET_4MA))
                            .setScale(0, RoundingMode.HALF_UP)
            ),
            new GasTypeConfig("0-10v",
                    (value, min, max) -> max.subtract(min)
                            .divide(new BigDecimal(RAW_FULL_SCALE_10BIT), 10, RoundingMode.HALF_UP)
                            .multiply(value)
                            .add(min),
                    (value, min, max) -> value.subtract(min)
                            .multiply(new BigDecimal(RAW_FULL_SCALE_10BIT))
                            .divide(max.subtract(min), 10, RoundingMode.HALF_UP)
                            .setScale(0, RoundingMode.HALF_UP)
            ),
            new GasTypeConfig("5-10v",
                    (value, min, max) -> max.subtract(min)
                            .divide(new BigDecimal(RAW_FULL_SCALE_10BIT), 10, RoundingMode.HALF_UP)
                            .multiply(value)
                            .add(min),
                    (value, min, max) -> value.subtract(min)
                            .multiply(new BigDecimal(RAW_FULL_SCALE_10BIT))
                            .divide(max.subtract(min), 10, RoundingMode.HALF_UP)
                            .setScale(0, RoundingMode.HALF_UP)
            ),
            new GasTypeConfig("0-20ma",
                    (value, min, max) -> max.subtract(min)
                            .divide(new BigDecimal(RAW_FULL_SCALE_0_20MA), 10, RoundingMode.HALF_UP)
                            .multiply(value)
                            .add(min),
                    (value, min, max) -> value.subtract(min)
                            .multiply(new BigDecimal(RAW_FULL_SCALE_0_20MA))
                            .divide(max.subtract(min), 10, RoundingMode.HALF_UP)
                            .setScale(0, RoundingMode.HALF_UP)
            )
    ).collect(Collectors.toMap(t -> t.name, Function.identity()));

    /**
     * The values of gas_type in the config file.
     */
    public static final Set<String> GAS_TYPES = Collections.unmodifiableSet(new TreeSet<>(CONFIGS.keySet()));

    private record GasTypeConfig(
            String name,
            TriFunction<BigDecimal, BigDecimal, BigDecimal, BigDecimal> fromEvent,
            TriFunction<BigDecimal, BigDecimal, BigDecimal, BigDecimal> toCommand
    ) {
    }
}
