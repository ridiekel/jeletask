package io.github.ridiekel.jeletask.client.spec.component;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.SensorType;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import lombok.Getter;
import lombok.Setter;

/**
 * A sensor. The type in the config file picks the subclass, so the type of a sensor cannot change.
 */
@Getter
@Setter
public abstract class SensorSpec extends ComponentSpec {
    // Overrides the unit Home Assistant shows, some sensor types have a default
    @JsonProperty("ha_unit_of_measurement")
    private String haUnitOfMeasurement;

    public abstract SensorType getSensorType();

    @Override
    public final Function getFunction() {
        return Function.SENSOR;
    }

    @Override
    public String getType() {
        return getSensorType().name();
    }
}
