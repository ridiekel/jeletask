package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
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
    public String convertGet(ComponentSpec component, byte[] value) {
        return this.getStateCalculator(component).convertGet(component, value);
    }

    @Override
    public byte[] convertSet(ComponentSpec component, String value) {
        return this.getStateCalculator(component).convertSet(component, value);
    }

    @Override
    public String getDefaultState(ComponentSpec component) {
        return this.getStateCalculator(component).getDefaultState(component);
    }

    private StateCalculator getStateCalculator(ComponentSpec component) {
        return Optional.ofNullable(this.sensorTypeCalculators.get(component.getType())).orElseGet(() -> {
            LOG.warn(String.format("State calculator not found for component:\n\n        %s\n", component));
            return new StateCalculator() {
                @Override
                public String convertGet(ComponentSpec component, byte[] value) {
                    return null;
                }

                @Override
                public byte[] convertSet(ComponentSpec component, String value) {
                    return new byte[0];
                }

                @Override
                public NumberConverter getNumberConverter() {
                    return null;
                }

                @Override
                public boolean isValidState(String state) {
                    return false;
                }

                @Override
                public String getDefaultState(ComponentSpec component) {
                    return null;
                }
            };
        });
    }
}
