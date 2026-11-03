package io.github.ridiekel.jeletask.client.spec.component;

import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.SensorType;

/**
 * A sensor with a text value. The bridge creates these itself, for the bridge and for every device; the config file cannot.
 */
public class StringSensorSpec extends SensorSpec {
    @Override
    public SensorType getSensorType() {
        return SensorType.STRING;
    }
}
