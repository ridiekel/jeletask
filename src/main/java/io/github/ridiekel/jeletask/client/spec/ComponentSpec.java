package io.github.ridiekel.jeletask.client.spec;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ridiekel.jeletask.client.spec.config.ConfigProblems;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HomeAssistentAutoConfig.HADeviceType;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

/**
 * This class represents a Teletask component, being either a: relay, motor, mood, ... basically anything which can be controlled.
 * <p>
 * Every function has its own subclass that holds only the properties of the config file that apply to it.
 * Only the fields annotated with {@link JsonProperty} are read from the config file.
 */
@Setter
@Getter
public abstract class ComponentSpec {
    @JsonProperty("number")
    private int number;

    @JsonProperty("description")
    private String description = "";

    @JsonProperty("device")
    private String device;

    @JsonProperty("suggested_area")
    private String suggestedArea;

    //haType is the type as it is known to Home Assistant, it overrides the type derived from the function
    @JsonProperty("haType")
    @JsonAlias("hatype")
    private String haType;

    private State<?> state;

    private HAConfig<?> haPublishedConfig = null;

    public abstract Function getFunction();

    /**
     * @return The type as it is known to Teletask or Home Assistant, depending on the function. {@code null} when the function has no type.
     */
    public String getType() {
        return null;
    }

    /**
     * Reports what is wrong with the configured properties. Subclasses add their own checks to the ones of their parent.
     */
    public void validate(ConfigProblems problems) {
        if (this.number < 0) {
            problems.error("number", "must not be negative, negative numbers are reserved for the bridge and its devices");
        }
        if (this.haType != null && Arrays.stream(HADeviceType.values()).noneMatch(t -> t.toString().equalsIgnoreCase(this.haType))) {
            problems.error("haType", "'" + this.haType + "' is not a Home Assistant type, expected one of " + Arrays.toString(HADeviceType.values()));
        }
    }

    // A component is identified by its function and number, the configured properties do not matter
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComponentSpec that)) return false;

        return number == that.number && getFunction() == that.getFunction();
    }

    @Override
    public final int hashCode() {
        int result = getFunction().hashCode();
        result = 31 * result + number;
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "function=" + getFunction() +
                ", number=" + number +
                ", description='" + description + '\'' +
                ", device='" + device + '\'' +
                ", type='" + getType() + '\'' +
                ", state=" + state +
                '}';
    }
}
