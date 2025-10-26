package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.types;

import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.SensorType;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAReadOnlyConfig;

import java.util.Optional;

/**
 * <a href="https://www.home-assistant.io/integrations/sensor.mqtt/">MQTT Sensor Discovery</a>
 */
public class HASensorConfig extends HAReadOnlyConfig<HASensorConfig> {

    public static final int TEMPERATURE_MINIMUM = 10;
    public static final int TEMPERATURE_MAXIMUM = 30;

    public HASensorConfig(HAConfigParameters parameters) {
        super(parameters);

        if (isType(parameters, SensorType.TEMPERATURECONTROL)) {
            // HA 'climate'

            this.putInt("min_temp", TEMPERATURE_MINIMUM);
            this.putInt("max_temp", TEMPERATURE_MAXIMUM);
            this.putDouble("precision", parameters.getComponentSpec().getHA_temperature_step());
            this.putDouble("temp_step", parameters.getComponentSpec().getHA_temperature_step());
            this.putArray("modes", parameters.getComponentSpec().getHA_modes().split(","));
            this.put("current_temperature_topic", "~/state");
            this.put("temperature_state_topic", "~/state");
            this.put("temperature_command_topic", "~/set");
            this.put("power_state_topic", "~/state");

            // If we don't enable power_command_topic HA won't send an ON command before a COOL/HEAT/DRY/... command.
            // Teletask needs an ON command before any other mode is accepted.
            // This will result in a double OFF command when turning off from HA but I don't think there's an easy solution to this right now?
            this.put("power_command_topic", "~/set");

            this.put("payload_on", """
                    { "state": "ON" }
                    """);
            this.put("payload_off", """
                    { "state": "OFF" }
                    """);
            this.put("mode_state_topic", "~/state");
            this.put("mode_command_topic", "~/set");
            this.put("fan_mode_state_topic", "~/state");
            this.put("fan_mode_command_topic", "~/set");
            this.put("current_temperature_template", "{{ value_json.current_temperature }}");
            this.put("temperature_state_template", "{{ value_json.target_temperature }}");
            this.put("temperature_command_template", """
                    { "state": "ON", "action": "TARGET", "target_temperature": {{ value|float }} }
                    """);
            this.put("mode_state_template", """
                    {% if value_json.mode|upper == "VENT" %}
                        fan_only
                    {% else %}
                        {{ value_json.mode|lower }}
                    {% endif %}
                    """);
            this.put("mode_command_template", """
                    {% if value|lower == "fan_only" %}
                        {"state": "ON", "mode": "VENT"}
                    {% else %}
                        {"state": "ON", "mode": "{{ value | upper}}"}
                    {% endif %}
                    """);
            this.put("fan_mode_command_template", """
                    {"state": "ON", "fanspeed": "{{ value|upper }}"}
                    """);
            this.put("fan_mode_state_template", """
                    {{ value_json.fanspeed|lower }}
                    """);
            this.put("unit_of_measurement", Optional.ofNullable(parameters.getComponentSpec().getHA_unit_of_measurement()).orElse("°C"));
        } else if (isType(parameters, SensorType.PULSECOUNTER)) {
            // TODO: implement both current + total. Only current is implemented right now.
            if (parameters.getComponentSpec().getHA_unit_of_measurement() != null) {
                this.put("unit_of_measurement", parameters.getComponentSpec().getHA_unit_of_measurement());
            }

            this.put("value_template", "{{ value_json.current }}");
        } else {
            // Regular simple sensor
            this.put("value_template", "{{ value_json.state }}");

            if (isType(parameters, SensorType.TEMPERATURE)) {
                this.deviceClass("temperature");
                this.stateClass("measurement");
                this.put("unit_of_measurement", Optional.ofNullable(parameters.getComponentSpec().getHA_unit_of_measurement()).orElse("°C"));
            } else if (isType(parameters, SensorType.LIGHT)) {
                this.deviceClass("illuminance");
                this.stateClass("measurement");
                this.put("unit_of_measurement", Optional.ofNullable(parameters.getComponentSpec().getHA_unit_of_measurement()).orElse("lx"));
            } else {
                if (parameters.getComponentSpec().getHA_unit_of_measurement() != null) {
                    this.put("unit_of_measurement", parameters.getComponentSpec().getHA_unit_of_measurement());
                }
            }
        }
    }

    private static boolean isType(HAConfigParameters parameters, SensorType expected) {
        return expected == SensorType.from(parameters.getComponentSpec());
    }
}
