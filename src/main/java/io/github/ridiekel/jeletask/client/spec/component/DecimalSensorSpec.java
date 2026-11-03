package io.github.ridiekel.jeletask.client.spec.component;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ridiekel.jeletask.client.spec.config.ConfigProblems;
import lombok.Getter;
import lombok.Setter;

/**
 * A sensor whose value is rounded to a configurable number of decimals.
 */
@Getter
@Setter
public abstract class DecimalSensorSpec extends SensorSpec {
    // Defaulting to 1 for temperature sensors. Otherwise we would have to always override this.
    @JsonProperty("decimals")
    private Integer decimals = 1;

    @Override
    public void validate(ConfigProblems problems) {
        super.validate(problems);
        if (this.decimals == null || this.decimals < 0) {
            problems.error("decimals", "must be 0 or more, but is " + this.decimals);
        }
    }
}
