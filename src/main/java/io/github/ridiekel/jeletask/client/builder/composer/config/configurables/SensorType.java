package io.github.ridiekel.jeletask.client.builder.composer.config.configurables;

import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.component.SensorSpec;

public enum SensorType {
    STRING,
    TEMPERATURE,
    LIGHT,
    HUMIDITY,
    GAS,
    TEMPERATURECONTROL,
    PULSECOUNTER;

    public static SensorType from(ComponentSpec spec) {
        return spec instanceof SensorSpec sensor ? sensor.getSensorType() : null;
    }
}
