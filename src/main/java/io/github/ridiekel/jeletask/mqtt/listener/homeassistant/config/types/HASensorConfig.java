package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.types;

import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.SensorType;
import io.github.ridiekel.jeletask.client.spec.component.SensorSpec;
import io.github.ridiekel.jeletask.client.spec.component.TemperatureControlSpec;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAReadOnlyConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <a href="https://www.home-assistant.io/integrations/sensor.mqtt/">MQTT Sensor Discovery</a>
 */
public class HASensorConfig extends HAReadOnlyConfig<HASensorConfig> {
    private static final Logger LOG = LogManager.getLogger();
    private static final String STATE_TOPIC = "~/state";
    private static final String COMMAND_TOPIC = "~/set";
    private static final String UNIT_OF_MEASUREMENT = "unit_of_measurement";

    public HASensorConfig(HAConfigParameters parameters) {
        super(parameters);

        if (isType(parameters, SensorType.TEMPERATURECONTROL)) {
            this.climate(parameters);
        } else if (isType(parameters, SensorType.PULSECOUNTER)) {
            this.pulseCounter(parameters);
        } else {
            this.simpleSensor(parameters);
        }
    }

    /**
     * HA 'climate'
     */
    private void climate(HAConfigParameters parameters) {
        TemperatureControlSpec thermostat = (TemperatureControlSpec) parameters.getComponentSpec();
        this.putDouble("min_temp", thermostat.getHaMinTemp());
        this.putDouble("max_temp", thermostat.getHaMaxTemp());
        this.putDouble("precision", thermostat.getHaTemperatureStep());
        this.putDouble("temp_step", thermostat.getHaTemperatureStep());
        this.putArray("modes", thermostat.getHaModes().split(","));
        this.put("current_temperature_topic", STATE_TOPIC);
        this.put("temperature_state_topic", STATE_TOPIC);
        this.put("temperature_command_topic", COMMAND_TOPIC);
        this.put("power_state_topic", STATE_TOPIC);

        // If we don't enable power_command_topic HA won't send an ON command before a COOL/HEAT/DRY/... command.
        // Teletask needs an ON command before any other mode is accepted.
        // This will result in a double OFF command when turning off from HA but I don't think there's an easy solution to this right now?
        this.put("power_command_topic", COMMAND_TOPIC);

        this.put("payload_on", """
                { "state": "ON" }
                """);
        this.put("payload_off", """
                { "state": "OFF" }
                """);
        this.put("mode_state_topic", STATE_TOPIC);
        this.put("mode_command_topic", COMMAND_TOPIC);
        this.put("current_temperature_template", "{{ value_json.current_temperature }}");
        // While OFF the central reports its frost protection target (e.g. 5.0); 'None' clears the target in HA
        this.put("temperature_state_template", "{% if value_json.state == 'OFF' %}None{% else %}{{ value_json.target_temperature }}{% endif %}");
        this.put("temperature_command_template", """
                { "state": "ON", "action": "TARGET", "target_temperature": {{ value|float }} }
                """);
        // Teletask keeps reporting the last mode (e.g. HEAT) while the thermostat is OFF
        this.put("mode_state_template", """
                {% if value_json.state|upper == "OFF" %}
                    off
                {% elif value_json.mode|upper == "VENT" %}
                    fan_only
                {% else %}
                    {{ value_json.mode|lower }}
                {% endif %}
                """);
        this.put("mode_command_template", """
                {% if value|lower == "off" %}
                    {"state": "OFF"}
                {% elif value|lower == "fan_only" %}
                    {"state": "ON", "mode": "VENT"}
                {% else %}
                    {"state": "ON", "mode": "{{ value | upper}}"}
                {% endif %}
                """);
        this.putFanModes(thermostat);
        if (thermostat.isHaHvacAction()) {
            this.putHvacAction();
        }
        this.putPresetModes(thermostat);
        this.put(UNIT_OF_MEASUREMENT, Optional.ofNullable(thermostat.getHaUnitOfMeasurement()).orElse("°C"));
    }

    private void putFanModes(TemperatureControlSpec thermostat) {
        String fanModes = thermostat.getHaFanModes();
        if (fanModes != null && !fanModes.isBlank()) {
            this.putArray("fan_modes", fanModes.split(","));
            this.put("fan_mode_state_topic", STATE_TOPIC);
            this.put("fan_mode_command_topic", COMMAND_TOPIC);
            this.put("fan_mode_command_template", """
                    {"state": "ON", "fanspeed": "{{ value|upper }}"}
                    """);
            this.put("fan_mode_state_template", """
                    {{ value_json.fanspeed|lower }}
                    """);
        }
    }

    private void putHvacAction() {
        // Bit 0 of output_state is the thermostat's own heat/cool demand (seen on MICROS+: 8 = idle, 9 = demand)
        this.put("action_topic", STATE_TOPIC);
        this.put("action_template", """
                {% if value_json.state == 'OFF' %}off{% elif (value_json.output_state | int(0)) % 2 == 1 %}{{ 'cooling' if value_json.mode == 'COOL' else 'heating' }}{% else %}idle{% endif %}
                """);
    }

    private void putPresetModes(TemperatureControlSpec thermostat) {
        // The config file validation already rejects invalid entries, this only protects HA from components made in code
        Map<String, String> presets = thermostat.presetModes((entry, reason) -> LOG.warn("Ignoring ha_preset_modes entry '{}' of sensor {}: {}", entry, thermostat.getNumber(), reason));
        if (!presets.isEmpty()) {
            String toLabel = presets.entrySet().stream().map(e -> jinjaString(e.getValue()) + ": " + jinjaString(e.getKey())).collect(Collectors.joining(", ", "{", "}"));
            String toPreset = presets.entrySet().stream().map(e -> jinjaString(e.getKey()) + ": " + jinjaString(e.getValue())).collect(Collectors.joining(", ", "{", "}"));
            this.putArray("preset_modes", presets.keySet().toArray(String[]::new));
            this.put("preset_mode_state_topic", STATE_TOPIC);
            this.put("preset_mode_command_topic", COMMAND_TOPIC);
            this.put("preset_mode_value_template", "{% if value_json.state == 'OFF' %}none{% else %}{{ " + toLabel + ".get((value_json.preset or 'MANUAL')|upper, 'none') }}{% endif %}");
            this.put("preset_mode_command_template", "{\"state\": \"ON\", \"preset\": \"{{ " + toPreset + ".get(value, value|upper) }}\"}");
        }
    }

    private void pulseCounter(HAConfigParameters parameters) {
        // Only the current value is published; the total would need its own Home Assistant entity next to this one.
        String unit = ((SensorSpec) parameters.getComponentSpec()).getHaUnitOfMeasurement();
        if (unit != null) {
            this.put(UNIT_OF_MEASUREMENT, unit);
        }

        this.put("value_template", "{{ value_json.current }}");
    }

    /**
     * Regular simple sensor
     */
    private void simpleSensor(HAConfigParameters parameters) {
        this.put("value_template", "{{ value_json.state }}");

        String unit = ((SensorSpec) parameters.getComponentSpec()).getHaUnitOfMeasurement();
        if (isType(parameters, SensorType.TEMPERATURE)) {
            this.deviceClass("temperature");
            this.stateClass("measurement");
            this.put(UNIT_OF_MEASUREMENT, Optional.ofNullable(unit).orElse("°C"));
        } else if (isType(parameters, SensorType.LIGHT)) {
            this.deviceClass("illuminance");
            this.stateClass("measurement");
            this.put(UNIT_OF_MEASUREMENT, Optional.ofNullable(unit).orElse("lx"));
        } else if (unit != null) {
            this.put(UNIT_OF_MEASUREMENT, unit);
        }
    }

    private static boolean isType(HAConfigParameters parameters, SensorType expected) {
        return expected == SensorType.from(parameters.getComponentSpec());
    }

    private static String jinjaString(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }
}
