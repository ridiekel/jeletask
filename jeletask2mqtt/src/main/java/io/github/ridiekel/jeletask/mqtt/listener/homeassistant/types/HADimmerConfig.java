package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAReadWriteConfig;

public class HADimmerConfig extends HAReadWriteConfig<HADimmerConfig> {
    public HADimmerConfig(HAConfigParameters parameters) {
        super(parameters);
        this.put("schema", "template");
        this.put("command_on_template", "{\"state\": {%- if brightness is not defined -%}\"PREVIOUS_STATE\"{%- else -%}\"ON\", \"brightness\": {{ (brightness / 255 * 100) | round(0) }}{%- endif -%}}");
        this.put("command_off_template", "{\"state\": \"off\"}");
        this.put("state_template", "{% if ( value_json.state|lower == 'on'  ) %}on{% else %}off{% endif %}");
        this.put("brightness_template", "{{ ( value_json.brightness|int / 100 * 255 ) | round(0) }}");
    }
}
