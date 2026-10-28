package io.github.ridiekel.jeletask.mqtt.container.ha;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Setter
@Getter
public class Entity extends HAObject {
    private String entity_id;
    private String state;
    private Attributes attributes;

    @Setter
    @Getter
    public static class Attributes extends HAObject {
        private String friendly_name;
        // Everything else HA reports, e.g. a climate's temperature, hvac_action and preset_mode
        @JsonAnySetter
        @JsonAnyGetter
        private Map<String, Object> other = new LinkedHashMap<>();

        public Object get(String name) {
            return other.get(name);
        }
    }
}
