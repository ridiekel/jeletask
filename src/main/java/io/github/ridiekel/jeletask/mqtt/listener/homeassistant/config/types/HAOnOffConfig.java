package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAReadWriteConfig;

/**
 * <a href="https://www.home-assistant.io/integrations/switch.mqtt/">MQTT Switch Discovery</a>
 */
public class HAOnOffConfig extends HAReadWriteConfig<HAOnOffConfig> {
    public HAOnOffConfig(HAConfigParameters parameters) {
        super(parameters);

        this.put("schema", "json");
        this.put("payload_on", """
                { "state": "ON" }
                """);
        this.put("payload_off", """
                { "state": "OFF" }
                """);
        this.put("state_on", "ON");
        this.put("state_off", "OFF");
        this.put("value_template", "{{ value_json.state }}");

        if ("light".equalsIgnoreCase(parameters.getComponentSpec().getType())) {
            this.put("supported_color_modes", "onoff");
        }
    }
}
