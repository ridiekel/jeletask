package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAReadWriteConfig;

/**
 * <a href="https://www.home-assistant.io/integrations/switch.mqtt/">MQTT Switch Discovery</a>
 */
public class HASwitchConfig extends HAReadWriteConfig<HASwitchConfig> {
    public HASwitchConfig(HAConfigParameters parameters) {
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
    }
}
