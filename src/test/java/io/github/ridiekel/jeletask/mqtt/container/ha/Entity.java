package io.github.ridiekel.jeletask.mqtt.container.ha;

import lombok.Getter;
import lombok.Setter;

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

    }
}
