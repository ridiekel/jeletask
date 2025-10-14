package io.github.ridiekel.jeletask.client.builder.composer.config.configurables;

import io.github.ridiekel.jeletask.client.spec.ComponentSpec;

import java.util.Optional;

public enum SensorType {
    TEMPERATURE,
    LIGHT,
    HUMIDITY,
    GAS,
    TEMPERATURECONTROL,
    PULSECOUNTER;

    public static SensorType from(ComponentSpec spec) {
        return Optional.ofNullable(spec.getType()).map(SensorType::valueOf).orElse(null);
    }
}
