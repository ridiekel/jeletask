package io.github.ridiekel.jeletask.client.spec;

import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.SensorType;
import io.github.ridiekel.jeletask.client.spec.component.*;
import io.github.ridiekel.jeletask.client.spec.state.State;

/**
 * Creates the {@link ComponentSpec} subclass of a function, like the config file does. A sensor needs a subclass of {@link SensorSpec} instead.
 */
public final class TestComponents {
    private TestComponents() {
    }

    public static SensorSpec sensor(SensorType type) {
        return switch (type) {
            case STRING -> new StringSensorSpec();
            case TEMPERATURE -> new TemperatureSensorSpec();
            case LIGHT -> new LightSensorSpec();
            case HUMIDITY -> new HumiditySensorSpec();
            case GAS -> new GasSensorSpec();
            case TEMPERATURECONTROL -> new TemperatureControlSpec();
            case PULSECOUNTER -> new PulseCounterSpec();
        };
    }

    public static ComponentSpec of(Function function, State<?> state, int number) {
        ComponentSpec component = of(function, number);
        component.setState(state);
        return component;
    }

    public static ComponentSpec of(Function function, int number) {
        ComponentSpec component = switch (function) {
            case RELAY -> new RelaySpec();
            case DIMMER -> new DimmerSpec();
            case MOTOR -> new MotorSpec();
            case LOCMOOD -> new LocalMoodSpec();
            case TIMEDMOOD -> new TimedMoodSpec();
            case GENMOOD -> new GeneralMoodSpec();
            case FLAG -> new FlagSpec();
            case COND -> new ConditionSpec();
            case INPUT -> new InputSpec();
            case TIMEDFNC -> new TimedFunctionSpec();
            case DISPLAYMESSAGE -> new DisplayMessageSpec();
            case SENSOR -> throw new IllegalArgumentException("A sensor needs its type, create a subclass of SensorSpec instead");
        };
        component.setNumber(number);
        return component;
    }
}
