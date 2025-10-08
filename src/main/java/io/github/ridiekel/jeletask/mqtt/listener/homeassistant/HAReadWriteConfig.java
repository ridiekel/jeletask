package io.github.ridiekel.jeletask.mqtt.listener.homeassistant;

public class HAReadWriteConfig<T extends HAReadWriteConfig<T>> extends HAConfig<T> {
    public HAReadWriteConfig(HAConfigParameters parameters) {
        super(parameters);
        this.commandTopic("~/set");
    }

    public final T commandTopic(String value) {
        return this.put("command_topic", value);
    }
}
