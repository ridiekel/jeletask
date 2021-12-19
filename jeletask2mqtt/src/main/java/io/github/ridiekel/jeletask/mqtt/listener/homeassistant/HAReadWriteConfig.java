package io.github.ridiekel.jeletask.mqtt.listener.homeassistant;

import io.github.ridiekel.jeletask.model.spec.CentralUnit;
import io.github.ridiekel.jeletask.model.spec.ComponentSpec;

public class HAReadWriteConfig<T extends HAReadWriteConfig<T>> extends HAConfig<T> {
    public HAReadWriteConfig(HAConfigParameters parameters) {
        super(parameters);
        this.commandTopic("~/set");
    }

    public final T commandTopic(String value) {
        return this.put("command_topic", value);
    }
}
