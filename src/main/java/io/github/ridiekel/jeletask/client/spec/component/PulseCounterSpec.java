package io.github.ridiekel.jeletask.client.spec.component;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.SensorType;
import io.github.ridiekel.jeletask.client.spec.config.ConfigProblems;
import lombok.Getter;
import lombok.Setter;

/**
 * A pulse counter, e.g. an energy meter.
 */
@Getter
@Setter
public class PulseCounterSpec extends SensorSpec {
    // The total is the counted pulses divided by this
    @JsonProperty("pulses_per_unit")
    private int pulsesPerUnit = 1000;

    @Override
    public SensorType getSensorType() {
        return SensorType.PULSECOUNTER;
    }

    @Override
    public void validate(ConfigProblems problems) {
        super.validate(problems);
        if (this.pulsesPerUnit <= 0) {
            problems.error("pulses_per_unit", "must be greater than 0, but is " + this.pulsesPerUnit);
        }
    }
}
