package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.types;

import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAReadOnlyConfig;


public class HASensorConfig extends HAReadOnlyConfig<HASensorConfig> {

    public HASensorConfig(HAConfigParameters parameters) {
        super(parameters);

        if ("TEMPERATURECONTROL".equalsIgnoreCase(parameters.getComponentSpec().getType())) {
            // HA 'climate'

            this.putInt("min_temp", 10);
            this.putInt("max_temp", 30);
            this.putDouble("precision", 0.1);
            this.putArray("modes", parameters.getComponentSpec().getHA_modes().split(","));
            this.put("current_temperature_topic", "~/state");
            this.put("temperature_state_topic", "~/state");
            this.put("temperature_command_topic", "~/set");
            this.put("power_state_topic", "~/state");

            // If we don't enable power_command_topic HA won't send an ON command before a COOL/HEAT/DRY/... command.
            // Teletask needs an ON command before any other mode is accepted.
            // This will result in a double OFF command when turning off from HA but I don't think there's an easy solution to this right now?
            this.put("power_command_topic", "~/set");

            this.put("payload_on", "{\"state\": \"ON\"}");
            this.put("payload_off", "{\"state\": \"OFF\"}");
            this.put("mode_state_topic", "~/state");
            this.put("mode_command_topic", "~/set");
            this.put("fan_mode_state_topic", "~/state");
            this.put("fan_mode_command_topic", "~/set");
            this.put("current_temperature_template", "{{ value_json.current_temperature }}");
            this.put("temperature_state_template", "{{ value_json.target_temperature }}");
            this.put("temperature_command_template", "{\"target_temperature\": \"{{ value }}\"}");
            this.put("mode_state_template", "{% if value_json.mode|upper == \"VENT\" %}fan_only{% else %}{{ value_json.mode|lower }}{% endif %}");
            this.put("mode_command_template", "{% if value|lower == \"fan_only\" %}{\"state\": \"VENT\"}{% else %}{\"state\": \"{{ value | upper}}\"}{% endif %}");
            this.put("fan_mode_command_template", "{% if value|lower == \"auto\" %}{\"state\": \"SPAUTO\"}{% elif value|lower == \"low\" %}{\"state\": \"SPLOW\"}{% elif value|lower == \"med\" %}{\"state\": \"SPMED\"}{% elif value|lower == \"high\" %}{\"state\": \"SPHIGH\"}{% endif %}");
            this.put("fan_mode_state_template", "{% if value_json.fanspeed|upper == \"SPAUTO\" %}auto{% elif value_json.fanspeed|upper == \"SPLOW\" %}low{% elif value_json.fanspeed|upper == \"SPMED\" %}med{% elif value_json.fanspeed|upper == \"SPHIGH\" %}high{% endif %}");

        } else if ("PULSECOUNTER".equalsIgnoreCase(parameters.getComponentSpec().getType())) {

            // TODO: implement both current + total. Only current is implemented right now.
            if (parameters.getComponentSpec().getHA_unit_of_measurement() != null)
                this.put("unit_of_measurement", parameters.getComponentSpec().getHA_unit_of_measurement());

            this.put("value_template", "{{ value_json.current }}");
        } else {
            // Regular simple sensor
            if (parameters.getComponentSpec().getHA_unit_of_measurement() != null)
                this.put("unit_of_measurement", parameters.getComponentSpec().getHA_unit_of_measurement());

            this.put("value_template", "{{ value_json.state }}");
            if ("TEMPERATURE".equalsIgnoreCase(parameters.getComponentSpec().getType())) {
                this.put("device_class", "temperature");
                this.put("state_class", "measurement");
            }
            else if ("LIGHT".equalsIgnoreCase(parameters.getComponentSpec().getType())) {
                this.put("device_class", "illuminance");
                this.put("state_class", "measurement");
            }
        }
    }
}
