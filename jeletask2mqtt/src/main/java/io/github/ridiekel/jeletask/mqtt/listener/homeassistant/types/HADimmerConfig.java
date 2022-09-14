package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAReadWriteConfig;

public class HADimmerConfig extends HAReadWriteConfig<HADimmerConfig> {
    public HADimmerConfig(HAConfigParameters parameters) {
        super(parameters);
        this.putInt("brightness_scale", 100);
        this.put("on_command_type","brightness");
        this.put("brightness_command_topic", "~/set");
        this.put("brightness_state_topic", "~/state");
        this.put("payload_on", "1");
        this.put("payload_off", "0");
        this.put("state_value_template", "{% if value_json.state|int > 0 %}1{% else %}0{% endif %}");
        this.put("brightness_value_template", "{{ value_json.state|int }}");
    }
}
