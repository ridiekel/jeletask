package io.github.ridiekel.jeletask.mqtt.listener.homeassistant;

import io.github.ridiekel.jeletask.model.spec.CentralUnit;
import io.github.ridiekel.jeletask.model.spec.ComponentSpec;

public class HAReadOnlyConfig<T extends HAReadOnlyConfig<T>> extends HAConfig<T> {
    public HAReadOnlyConfig(HAConfigParameters parameters) {
        super(parameters);
    }
}
