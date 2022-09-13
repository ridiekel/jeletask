package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAReadWriteConfig;

public class HAMotorConfig extends HAReadWriteConfig<HAMotorConfig> {
    public HAMotorConfig(HAConfigParameters parameters) {
        super(parameters);
        this.put("position_topic", "~/state");
        this.put("set_position_topic", "~/set");
        this.put("command_topic", "~/set");
        this.putInt("position_open", 0);
        this.putInt("position_closed", 100);
        this.put("optimistic", "false");
        this.put("payload_open", "UP");
        this.put("payload_close", "DOWN");
        this.put("payload_stop", "STOP");
        this.put("position_template", "{{ value_json.position }}");
    }
}
