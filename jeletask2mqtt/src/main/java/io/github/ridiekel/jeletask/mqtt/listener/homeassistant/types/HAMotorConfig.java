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
        this.put("payload_open", "{\"state\": \"UP\"}");
        this.put("payload_close", "{\"state\": \"DOWN\"}");
        this.put("payload_stop", "{\"state\": \"STOP\"}");
        this.put("position_template", "{{ value_json.position }}");
        this.put("set_position_template", "{\"position\": {{ 100 - position }}}");
        this.put("value_template", "{% if value_json.state == 'OFF' %}{% if value_json.position == 100 %}closed{% elif value_json.position == 0 %}open{% else %}stopped{% endif %}{% else %}{% if value_json.last_direction == 'DOWN' %}closing{% else %}opening{% endif %}{% endif %}");
    }
}
