package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAReadWriteConfig;

public class HARelayConfig extends HAReadWriteConfig<HARelayConfig> {
    public HARelayConfig(HAConfigParameters parameters) {
        super(parameters);

        this.put("payload_on", "{\"state\": \"ON\"}");
        this.put("payload_off", "{\"state\": \"OFF\"}");

        if ("light".equalsIgnoreCase(parameters.getComponentSpec().getType())) {

            this.put("state_value_template", "{% if value_json.state|upper == \"ON\" %}{\"state\": \"ON\"}{% else %}{\"state\": \"OFF\"}{% endif %}");
        } else {
            this.put("value_template", "{{ value_json.state }}");
            this.put("state_on", "ON");
            this.put("state_off", "OFF");
        }

    }
}
