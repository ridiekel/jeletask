package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAReadOnlyConfig;

/**
 * <a href="https://www.home-assistant.io/integrations/sensor.mqtt/">MQTT Sensor Discovery</a>
 */
public class HAInputTriggerConfig extends HAReadOnlyConfig<HAInputTriggerConfig> {
    public HAInputTriggerConfig(HAConfigParameters parameters) {
        super(parameters);

        this.put("value_template", "{{ value_json.state }}");
    }
}
