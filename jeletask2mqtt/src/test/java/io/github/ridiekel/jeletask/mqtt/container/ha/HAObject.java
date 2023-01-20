package io.github.ridiekel.jeletask.mqtt.container.ha;

import com.fasterxml.jackson.core.JsonProcessingException;

public abstract class HAObject {
    @Override
    public String toString() {
        String result;
        try {
            result = HomeAssistantContainer.OBJECT_WRITER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
