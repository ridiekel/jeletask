package io.github.ridiekel.jeletask.mqtt.listener.homeassistant;

public class HAReadOnlyConfig<T extends HAReadOnlyConfig<T>> extends HAConfig<T> {
    public HAReadOnlyConfig(HAConfigParameters parameters) {
        super(parameters);
    }
}
