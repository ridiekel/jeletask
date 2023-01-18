package io.github.ridiekel.jeletask.mqtt.container.ha;

import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;

public class Entity extends HAObject {
    private String entity_id;
    private String state;
    private Attributes attribute;

    public String getEntity_id() {
        return entity_id;
    }

    public String getState() {
        return state;
    }

    public Attributes getAttribute() {
        return attribute;
    }

    public static class Attributes extends HAObject {
        private String friendly_name;

        public String getFriendly_name() {
            return friendly_name;
        }
    }
}
