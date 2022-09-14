package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAReadWriteConfig;

public class HARelayConfig extends HAReadWriteConfig<HARelayConfig> {
    public HARelayConfig(HAConfigParameters parameters) {
        super(parameters);
        switch ( parameters.getComponentSpec().getType().toLowerCase() ) {
            case "light":
                this.put("state_value_template", "{{ value_json.state }}");
                break;
            default:
                this.put("value_template", "{{ value_json.state }}");
                break;
        }
    }
}
