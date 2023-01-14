package io.github.ridiekel.jeletask.mqtt.container.ha;

public record Entity(
        String entity_id,
        String state,
        Attributes attributes
) {
    public record Attributes(
            String friendly_name
    ) {
    }
}
