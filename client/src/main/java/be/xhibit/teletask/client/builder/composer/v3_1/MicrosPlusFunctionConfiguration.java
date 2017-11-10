package be.xhibit.teletask.client.builder.composer.v3_1;

import be.xhibit.teletask.client.builder.composer.config.ConfigurationSupport;
import be.xhibit.teletask.client.builder.composer.config.NumberConverter;
import be.xhibit.teletask.client.builder.composer.config.configurables.FunctionConfigurable;
import be.xhibit.teletask.client.builder.composer.config.statecalculator.DimmerStateCalculator;
import be.xhibit.teletask.client.builder.composer.config.statecalculator.HumidityStateCalculator;
import be.xhibit.teletask.client.builder.composer.config.statecalculator.LuxStateCalculator;
import be.xhibit.teletask.client.builder.composer.config.statecalculator.MotorStateCalculator;
import be.xhibit.teletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import be.xhibit.teletask.client.builder.composer.config.statecalculator.SensorStateCalculator;
import be.xhibit.teletask.client.builder.composer.config.statecalculator.StateCalculator;
import be.xhibit.teletask.client.builder.composer.config.statecalculator.TemperatureStateCalculator;
import be.xhibit.teletask.model.spec.Function;
import com.google.common.collect.ImmutableList;

public class MicrosPlusFunctionConfiguration extends ConfigurationSupport<Function, FunctionConfigurable, Integer> {
    private static final StateCalculator ON_OFF_TOGGLE = new OnOffToggleStateCalculator(NumberConverter.UNSIGNED_BYTE, 255, 0, 103);

    public MicrosPlusFunctionConfiguration() {
        super(ImmutableList.<FunctionConfigurable>builder()
                .add(new FunctionConfigurable(Function.RELAY, 1, ON_OFF_TOGGLE))
                .add(new FunctionConfigurable(Function.DIMMER, 2, new DimmerStateCalculator(NumberConverter.UNSIGNED_BYTE)))
                .add(new FunctionConfigurable(Function.MOTOR, 6, new MotorStateCalculator(NumberConverter.UNSIGNED_BYTE, 1, 2, 3)))
                .add(new FunctionConfigurable(Function.LOCMOOD, 8, ON_OFF_TOGGLE))
                .add(new FunctionConfigurable(Function.TIMEDMOOD, 9, ON_OFF_TOGGLE))
                .add(new FunctionConfigurable(Function.GENMOOD, 10, ON_OFF_TOGGLE))
                .add(new FunctionConfigurable(Function.FLAG, 15, ON_OFF_TOGGLE))
                .add(new FunctionConfigurable(Function.SENSOR, 20, new SensorStateCalculator(
                        new TemperatureStateCalculator(NumberConverter.UNSIGNED_SHORT, 10, 273),
                        new LuxStateCalculator(NumberConverter.UNSIGNED_SHORT),
                        new HumidityStateCalculator(NumberConverter.UNSIGNED_SHORT)
                )))
                .add(new FunctionConfigurable(Function.COND, 60, ON_OFF_TOGGLE))
                .build());
    }

    @Override
    protected Integer getKey(FunctionConfigurable configurable) {
        return configurable.getNumber();
    }
}
