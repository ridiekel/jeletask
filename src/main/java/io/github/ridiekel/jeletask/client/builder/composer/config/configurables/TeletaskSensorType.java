package io.github.ridiekel.jeletask.client.builder.composer.config.configurables;

import io.github.ridiekel.jeletask.client.spec.ComponentSpec;

import java.util.Optional;

public enum TeletaskSensorType {
    TEMPERATURE,
    LIGHT,
    HUMIDITY,
    GAS,
    TEMPERATURECONTROL,
    PULSECOUNTER;

    public static TeletaskSensorType from(ComponentSpec spec) {
        return Optional.ofNullable(spec.getType()).map(TeletaskSensorType::valueOf).orElse(null);
    }
}
