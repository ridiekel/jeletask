package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAReadOnlyConfig;

/**
 * <a href="https://www.home-assistant.io/integrations/sensor.mqtt/">MQTT Sensor Discovery</a>
 */
public class HAInputTriggerConfig extends HAReadOnlyConfig<HAInputTriggerConfig> {
    public HAInputTriggerConfig(HAConfigParameters parameters) {
        super(parameters);

        this.put("value_template", "{{ value_json.state }}");
    }
}
