package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAReadWriteConfig;

public class HARelayConfig extends HAReadWriteConfig<HARelayConfig> {
    public HARelayConfig(HAConfigParameters parameters) {
        super(parameters);
        if ("light".equalsIgnoreCase(parameters.getComponentSpec().getType())) {
            this.put("state_value_template", "{{ value_json.state }}");
        } else {
            this.put("value_template", "{{ value_json.state }}");
        }
    }
}
