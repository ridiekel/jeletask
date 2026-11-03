package io.github.ridiekel.jeletask.client.spec.component;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.config.ConfigProblems;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HomeAssistentAutoConfig.HADeviceType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * A function that can only be turned on or off. The type picks how Home Assistant shows it.
 */
@Getter
@Setter
public abstract class OnOffSpec extends ComponentSpec {
    private static final List<HADeviceType> VALID_TYPES = List.of(HADeviceType.LIGHT, HADeviceType.SWITCH, HADeviceType.SCENE, HADeviceType.BINARY_SENSOR);

    @JsonProperty("type")
    private String type;

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public void validate(ConfigProblems problems) {
        super.validate(problems);
        if (this.type != null && VALID_TYPES.stream().noneMatch(t -> t.toString().equalsIgnoreCase(this.type))) {
            problems.error("type", "'" + this.type + "' is not supported for a " + getFunction() + ", expected one of " + VALID_TYPES);
        }
    }
}
