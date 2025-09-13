package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAReadWriteConfig;

public class HARelayConfig extends HAReadWriteConfig<HARelayConfig> {
    public HARelayConfig(HAConfigParameters parameters) {
        super(parameters);

        if ("light".equalsIgnoreCase(parameters.getComponentSpec().getType())) {
            // Code analog to dimmer implementation
            this.put("schema", "template");
            this.put("command_on_template", "{\"state\": \"on\"}");
            this.put("command_off_template", "{\"state\": \"off\"}");
            this.put("state_template", "{% if value_json.state|lower == 'on' %}on{% else %}off{% endif %}");
        } else {
            this.put("payload_on", "{\"state\": \"ON\"}");
            this.put("payload_off", "{\"state\": \"OFF\"}");

            this.put("state_value_template", "{{ value_json.state }}");
            this.put("state_on", "ON");
            this.put("state_off", "OFF");
        }

    }
}
