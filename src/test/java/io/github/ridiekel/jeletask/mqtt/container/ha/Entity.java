package io.github.ridiekel.jeletask.mqtt.container.ha;

public class Entity extends HAObject {
    private String entity_id;
    private String state;
    private Attributes attributes;

    public String getEntity_id() {
        return entity_id;
    }

    public String getState() {
        return state;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public void setEntity_id(String entity_id) {
        this.entity_id = entity_id;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    public static class Attributes extends HAObject {
        private String friendly_name;

        public String getFriendly_name() {
            return friendly_name;
        }

        public void setFriendly_name(String friendly_name) {
            this.friendly_name = friendly_name;
        }
    }
}
