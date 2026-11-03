package io.github.ridiekel.jeletask.client.spec.component;

import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.SensorType;

/**
 * A light sensor, in lx unless ha_unit_of_measurement says otherwise.
 */
public class LightSensorSpec extends SensorSpec {
    @Override
    public SensorType getSensorType() {
        return SensorType.LIGHT;
    }
}
