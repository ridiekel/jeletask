package io.github.ridiekel.jeletask.client.builder.composer.microsplus;

import io.github.ridiekel.jeletask.client.builder.composer.config.ConfigurationSupport;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.FunctionConfigurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.TeletaskSensorType;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.*;
import io.github.ridiekel.jeletask.client.spec.Function;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MicrosPlusFunctionConfiguration extends ConfigurationSupport<Function, FunctionConfigurable, Integer> {
    private static final OnOffToggleStateCalculator ON_OFF_TOGGLE = new OnOffToggleStateCalculator();
    private static final Map<TeletaskSensorType, StateCalculator<?>> SENSOR_CALCULATORS = Map.of(
            TeletaskSensorType.TEMPERATURE, new TemperatureStateCalculator(),
            TeletaskSensorType.LIGHT, new LuxStateCalculator(),
            TeletaskSensorType.HUMIDITY, new HumidityStateCalculator(),
            TeletaskSensorType.GAS, new GasStateCalculator(),
            TeletaskSensorType.TEMPERATURECONTROL, new TemperatureControlStateCalculator(),
            TeletaskSensorType.PULSECOUNTER, new PulseCounterStateCalculator()
    );

    public MicrosPlusFunctionConfiguration() {
        super(List.of(
                new FunctionConfigurable(Function.RELAY, 1, ON_OFF_TOGGLE),
                new FunctionConfigurable(Function.DIMMER, 2, new DimmerStateCalculator()),
                new FunctionConfigurable(Function.MOTOR, 6, new MotorStateCalculator()),
                new FunctionConfigurable(Function.LOCMOOD, 8, ON_OFF_TOGGLE),
                new FunctionConfigurable(Function.TIMEDMOOD, 9, ON_OFF_TOGGLE),
                new FunctionConfigurable(Function.GENMOOD, 10, ON_OFF_TOGGLE),
                new FunctionConfigurable(Function.FLAG, 15, ON_OFF_TOGGLE),
                new FunctionConfigurable(Function.SENSOR, 20, c -> Optional.ofNullable(SENSOR_CALCULATORS.get(TeletaskSensorType.from(c))).orElseThrow(() -> new IllegalArgumentException("Cannot find sensor state calculator for component " + c))),
                new FunctionConfigurable(Function.COND, 60, ON_OFF_TOGGLE),
                new FunctionConfigurable(Function.INPUT, 52, new InputStateCalculator()),
                new FunctionConfigurable(Function.TIMEDFNC, 5, ON_OFF_TOGGLE),
                new FunctionConfigurable(Function.DISPLAYMESSAGE, 54, new DisplayMessageStateCalculator())
        ));
    }

    @Override
    protected Integer getKey(FunctionConfigurable configurable) {
        return configurable.getNumber();
    }
}
