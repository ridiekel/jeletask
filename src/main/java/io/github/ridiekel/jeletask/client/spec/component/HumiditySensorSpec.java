package io.github.ridiekel.jeletask.client.spec.component;

import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.SensorType;

/**
 * A humidity sensor.
 */
public class HumiditySensorSpec extends SensorSpec {
    @Override
    public SensorType getSensorType() {
        return SensorType.HUMIDITY;
    }
}
