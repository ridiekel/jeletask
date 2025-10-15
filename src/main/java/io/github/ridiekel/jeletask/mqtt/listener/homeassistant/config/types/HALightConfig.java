package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAReadWriteConfig;

/**
 * <a href="https://www.home-assistant.io/integrations/light.mqtt/#json-schema">MQTT Light Discovery</a>
 */
public class HALightConfig extends HAReadWriteConfig<HALightConfig> {

    public HALightConfig(HAConfigParameters parameters) {
        super(parameters);

        this.put("schema", "json");
        this.put("payload_on", "ON");
        this.put("payload_off", "OFF");
        this.put("value_template", "{{ value_json.state }}");
        this.put("state_value_template", "{{ value_json.state }}");
    }
}
