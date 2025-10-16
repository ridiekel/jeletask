package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.GasState;
import org.apache.commons.lang3.function.TriFunction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
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
        BigDecimal min = Optional.ofNullable(component.getGas_min()).orElseThrow(() -> new IllegalArgumentException("Gas min cannot be null"));
        BigDecimal max = Optional.ofNullable(component.getGas_max()).orElseThrow(() -> new IllegalArgumentException("Gas max cannot be null"));

        BigDecimal calculated = Optional.ofNullable(component.getGas_type()).map(CONFIGS::get).map(c -> c.fromEvent.apply(value, min, max)).orElseThrow(() -> new IllegalArgumentException("Gas type '" + component.getGas_type() + "' not found in: " + CONFIGS));

        return calculated.setScale(Optional.ofNullable(component.getDecimals()).orElse(2), RoundingMode.HALF_UP);
    }

    @Override
    public byte[] toCommand(ComponentSpec component, GasState state) {
        BigDecimal value = state.getState();

        BigDecimal calculated = toCommandValue(component, value);

        return NumberConverter.UNSIGNED_SHORT.convert(calculated);
    }

    public static BigDecimal toCommandValue(ComponentSpec component, BigDecimal value) {
        BigDecimal min = Optional.ofNullable(component.getGas_min()).orElseThrow(() -> new IllegalArgumentException("Gas min cannot be null"));
        BigDecimal max = Optional.ofNullable(component.getGas_max()).orElseThrow(() -> new IllegalArgumentException("Gas max cannot be null"));

        return Optional.ofNullable(component.getGas_type()).map(CONFIGS::get).map(c -> c.toCommand.apply(value, min, max)).orElseThrow(() -> new IllegalArgumentException("Gas type '" + component.getGas_type() + "' not found in: " + CONFIGS));
    }

    @Override
    protected Class<GasState> getStateType() {
        return GasState.class;
    }

    private static final Map<String, GasTypeConfig> CONFIGS = Stream.of(
            new GasTypeConfig(
                    "4-20ma",
                    (value, min, max) -> max.subtract(min)
                            .divide(new BigDecimal("704.0"), 10, RoundingMode.HALF_UP)
                            .multiply(value.subtract(new BigDecimal("176")))
                            .add(min),
                    (value, min, max) -> value.subtract(min)
                            .multiply(new BigDecimal("704.0"))
                            .divide(max.subtract(min), 10, RoundingMode.HALF_UP)
                            .add(new BigDecimal("176"))
                            .setScale(0, RoundingMode.HALF_UP)
            ),
            new GasTypeConfig(
                    "0-10v",
                    (value, min, max) -> max.subtract(min)
                            .divide(new BigDecimal("1023.0"), 10, RoundingMode.HALF_UP)
                            .multiply(value)
                            .add(min),
                    (value, min, max) -> value.subtract(min)
                            .multiply(new BigDecimal("1023.0"))
                            .divide(max.subtract(min), 10, RoundingMode.HALF_UP)
                            .setScale(0, RoundingMode.HALF_UP)
            ),
            new GasTypeConfig(
                    "5-10v",
                    (value, min, max) -> max.subtract(min)
                            .divide(new BigDecimal("1023.0"), 10, RoundingMode.HALF_UP)
                            .multiply(value)
                            .add(min),
                    (value, min, max) -> value.subtract(min)
                            .multiply(new BigDecimal("1023.0"))
                            .divide(max.subtract(min), 10, RoundingMode.HALF_UP)
                            .setScale(0, RoundingMode.HALF_UP)
            ),
            new GasTypeConfig(
                    "0-20ma",
                    (value, min, max) -> max.subtract(min)
                            .divide(new BigDecimal("880.0"), 10, RoundingMode.HALF_UP)
                            .multiply(value)
                            .add(min),
                    (value, min, max) -> value.subtract(min)
                            .multiply(new BigDecimal("880.0"))
                            .divide(max.subtract(min), 10, RoundingMode.HALF_UP)
                            .setScale(0, RoundingMode.HALF_UP)
            )
    ).collect(Collectors.toMap(t -> t.name, Function.identity()));

    private record GasTypeConfig(
            String name,
            TriFunction<BigDecimal, BigDecimal, BigDecimal, BigDecimal> fromEvent,
            TriFunction<BigDecimal, BigDecimal, BigDecimal, BigDecimal> toCommand
    ) {
    }
}
