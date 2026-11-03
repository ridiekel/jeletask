package io.github.ridiekel.jeletask.client.spec.component;

import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.SensorType;

/**
 * A temperature sensor, in °C unless ha_unit_of_measurement says otherwise.
 */
public class TemperatureSensorSpec extends DecimalSensorSpec {
    @Override
    public SensorType getSensorType() {
        return SensorType.TEMPERATURE;
    }
}
