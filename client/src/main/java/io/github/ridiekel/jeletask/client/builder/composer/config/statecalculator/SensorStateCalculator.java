package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.model.spec.ComponentSpec;

import java.util.Map;

public class SensorStateCalculator extends SimpleStateCalculator {
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
        return this.sensorTypeCalculators.get(component.getType());
    }
}
