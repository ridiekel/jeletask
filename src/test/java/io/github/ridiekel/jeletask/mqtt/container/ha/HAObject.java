package io.github.ridiekel.jeletask.mqtt.container.ha;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class HAObject {
    @Override
    public String toString() {
        String result;
        try {
            result = HomeAssistantContainer.OBJECT_WRITER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    public String toPrettyString() {
        String result;
        try {
            result = HomeAssistantContainer.OBJECT_WRITER.withDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }
}
