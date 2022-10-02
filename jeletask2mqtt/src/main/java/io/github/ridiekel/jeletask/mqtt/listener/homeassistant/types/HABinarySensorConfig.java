package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAReadWriteConfig;


public class HABinarySensorConfig extends HAReadWriteConfig<HARelayConfig> {
    public HABinarySensorConfig(HAConfigParameters parameters) {
        super(parameters);

        this.put("payload_on", "OPEN");
        this.put("payload_off", "CLOSED");
        this.put("value_template", "{{ value_json.state }}");
    }
}
