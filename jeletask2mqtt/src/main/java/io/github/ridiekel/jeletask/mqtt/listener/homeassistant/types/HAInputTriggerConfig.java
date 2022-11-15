package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAReadOnlyConfig;


public class HAInputTriggerConfig extends HAReadOnlyConfig<HAInputTriggerConfig> {
    public HAInputTriggerConfig(HAConfigParameters parameters) {
        super(parameters);

        this.put("value_template", "{{ value_json.state }}");
    }
}
