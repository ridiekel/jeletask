package io.jeletask.teletask.client.builder.composer.v3_1;

import io.jeletask.teletask.client.builder.composer.config.ConfigurationSupport;
import io.jeletask.teletask.client.builder.composer.config.NumberConverter;
import io.jeletask.teletask.client.builder.composer.config.configurables.FunctionConfigurable;
import io.jeletask.teletask.client.builder.composer.config.statecalculator.DimmerStateCalculator;
import io.jeletask.teletask.client.builder.composer.config.statecalculator.HumidityStateCalculator;
import io.jeletask.teletask.client.builder.composer.config.statecalculator.LuxStateCalculator;
import io.jeletask.teletask.client.builder.composer.config.statecalculator.MotorStateCalculator;
import io.jeletask.teletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.jeletask.teletask.client.builder.composer.config.statecalculator.SensorStateCalculator;
import io.jeletask.teletask.client.builder.composer.config.statecalculator.StateCalculator;
import io.jeletask.teletask.client.builder.composer.config.statecalculator.TemperatureStateCalculator;
import io.jeletask.teletask.model.spec.Function;

import java.util.List;

public class MicrosPlusFunctionConfiguration extends ConfigurationSupport<Function, FunctionConfigurable, Integer> {
    private static final StateCalculator ON_OFF_TOGGLE = new OnOffToggleStateCalculator(NumberConverter.UNSIGNED_BYTE, 255, 0, 103);

    public MicrosPlusFunctionConfiguration() {
        super(List.of(
                new FunctionConfigurable(Function.RELAY, 1, ON_OFF_TOGGLE),
                new FunctionConfigurable(Function.DIMMER, 2, new DimmerStateCalculator(NumberConverter.UNSIGNED_BYTE)),
                new FunctionConfigurable(Function.MOTOR, 6, new MotorStateCalculator(NumberConverter.UNSIGNED_BYTE, 1, 2, 3)),
                new FunctionConfigurable(Function.LOCMOOD, 8, ON_OFF_TOGGLE),
                new FunctionConfigurable(Function.TIMEDMOOD, 9, ON_OFF_TOGGLE),
                new FunctionConfigurable(Function.GENMOOD, 10, ON_OFF_TOGGLE),
                new FunctionConfigurable(Function.FLAG, 15, ON_OFF_TOGGLE),
                new FunctionConfigurable(Function.SENSOR, 20, new SensorStateCalculator(
                        new TemperatureStateCalculator(NumberConverter.UNSIGNED_SHORT, 10, 273),
                        new LuxStateCalculator(NumberConverter.UNSIGNED_SHORT),
                        new HumidityStateCalculator(NumberConverter.UNSIGNED_SHORT)
                )),
                new FunctionConfigurable(Function.COND, 60, ON_OFF_TOGGLE)
        ));
    }

    @Override
    protected Integer getKey(FunctionConfigurable configurable) {
        return configurable.getNumber();
    }
}
