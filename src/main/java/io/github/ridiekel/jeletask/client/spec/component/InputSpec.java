package io.github.ridiekel.jeletask.client.spec.component;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.config.ConfigProblems;
import lombok.Getter;
import lombok.Setter;

/**
 * An input, read only. With a long press duration the bridge tells a short press from a long one.
 */
@Getter
@Setter
public class InputSpec extends ComponentSpec {
    @JsonProperty("long_press_duration_millis")
    private Integer longPressDurationMillis;

    @Override
    public Function getFunction() {
        return Function.INPUT;
    }

    @Override
    public void validate(ConfigProblems problems) {
        super.validate(problems);
        if (this.longPressDurationMillis != null && this.longPressDurationMillis <= 0) {
            problems.error("long_press_duration_millis", "must be greater than 0, but is " + this.longPressDurationMillis);
        }
    }
}
