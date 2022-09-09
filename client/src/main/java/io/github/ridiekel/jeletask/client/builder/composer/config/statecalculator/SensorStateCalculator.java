package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

public class SensorStateCalculator extends SimpleStateCalculator {
    private static final Logger LOG = LoggerFactory.getLogger(SensorStateCalculator.class);

    private final Map<String, StateCalculator> sensorTypeCalculators;

    public SensorStateCalculator(StateCalculator temperature, StateCalculator lux, StateCalculator humidity) {
        super(temperature.getNumberConverter());
        this.sensorTypeCalculators = Map.of(
                "TEMPERATURE", temperature,
                "LIGHT", lux,
                "HUMIDITY", humidity
        );
    }

    @Override
    public ComponentState toComponentState(byte[] dataBytes) {
        throw new IllegalStateException("Should not get here");
    }

    @Override
    public byte[] toBytes(ComponentState value) {
        throw new IllegalStateException("Should not get here");
    }

    @Override
    public ComponentState getDefaultState(ComponentSpec component) {
        throw new IllegalStateException("Should not get here");
    }

    @Override
    public StateCalculator forComponent(ComponentSpec component) {
        return Optional.ofNullable(this.sensorTypeCalculators.get(component.getType())).orElseGet(() -> {
            LOG.warn(String.format("State calculator not found for component:\n\n        %s\n", component));
            return new StateCalculator() {
                @Override
                public ComponentState toComponentState(byte[] value) {
                    return null;
                }

                @Override
                public byte[] toBytes(ComponentState value) {
                    return Bytes.EMPTY;
                }

                @Override
                public NumberConverter getNumberConverter() {
                    return null;
                }

                @Override
                public boolean isValidState(ComponentState state) {
                    return false;
                }

                @Override
                public ComponentState getDefaultState(ComponentSpec component) {
                    return null;
                }
            };
        });
    }
}
