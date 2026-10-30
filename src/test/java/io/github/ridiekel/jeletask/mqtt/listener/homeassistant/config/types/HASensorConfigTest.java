package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.CentralUnitType;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HomeAssistentAutoConfig;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfigParameters;
import io.github.ridiekel.jeletask.utilities.StringUtilities;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

class HASensorConfigTest {
    private static final String IDENTIFIER = "MAN_TEST_localhost";

    private static ObjectNode config(ComponentSpec component, HomeAssistentAutoConfig.HADeviceType deviceType) {
        CentralUnit centralUnit = new CentralUnit();
        centralUnit.setType(CentralUnitType.MICROS_PLUS);
        centralUnit.setVersion("test");
        Map<Function, List<ComponentSpec>> components = new LinkedHashMap<>();
        components.put(Function.SENSOR, List.of(component));
        centralUnit.setComponentsTypes(components);
        centralUnit.getAllComponents();

        HAConfigParameters parameters = new HAConfigParameters(new Teletask2MqttConfigurationProperties(), centralUnit, component, "teletask2mqtt/" + IDENTIFIER, null, IDENTIFIER, deviceType, null);
        return new HASensorConfig(parameters).getConfig();
    }

    private static ObjectNode thermostat(Consumer<ComponentSpec> configure) {
        ComponentSpec component = new ComponentSpec();
        component.setNumber(33);
        component.setDescription("Temperature Control");
        component.setType("TEMPERATURECONTROL");
        configure.accept(component);
        return config(component, HomeAssistentAutoConfig.HADeviceType.CLIMATE);
    }

    private static List<String> texts(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false).map(JsonNode::asText).toList();
    }

    @Test
    void defaultsAddNoPresetsAndNoHvacAction() {
        ObjectNode config = thermostat(c -> {
        });

        Assertions.assertThat(config.has("preset_modes")).isFalse();
        Assertions.assertThat(config.has("action_topic")).isFalse();
        Assertions.assertThat(config.has("action_template")).isFalse();
        Assertions.assertThat(texts(config.get("fan_modes"))).containsExactly("auto", "low", "medium", "high");
        Assertions.assertThat(config.get("min_temp").asDouble()).isEqualTo(10);
        Assertions.assertThat(config.get("max_temp").asDouble()).isEqualTo(30);
    }

    @Test
    void optionsAreReadFromTheConfigKeys() throws Exception {
        ComponentSpec component = StringUtilities.OBJECT_MAPPER.readValue("{\"ha_preset_modes\": \"Dag=DAY\", \"ha_hvac_action\": true}", ComponentSpec.class);

        Assertions.assertThat(component.getHA_preset_modes()).isEqualTo("Dag=DAY");
        Assertions.assertThat(component.isHA_hvac_action()).isTrue();
    }

    @Test
    void presetsWithLabels() {
        ObjectNode config = thermostat(c -> c.setHA_preset_modes("Manual=MANUAL, Comfort=day ,Night=NIGHT,ECO"));

        Assertions.assertThat(texts(config.get("preset_modes"))).containsExactly("Manual", "Comfort", "Night", "ECO");
        Assertions.assertThat(config.get("preset_mode_value_template").asText()).contains("{'MANUAL': 'Manual', 'DAY': 'Comfort', 'NIGHT': 'Night', 'ECO': 'ECO'}");
        Assertions.assertThat(config.get("preset_mode_command_template").asText()).contains("{'Manual': 'MANUAL', 'Comfort': 'DAY', 'Night': 'NIGHT', 'ECO': 'ECO'}");
    }

    @Test
    void unknownPresetsAndTheReservedLabelNoneAreLeftOut() {
        ObjectNode config = thermostat(c -> c.setHA_preset_modes("Dag=DAY,Nacht=NACHT,none=ECO"));

        Assertions.assertThat(texts(config.get("preset_modes"))).containsExactly("Dag");
    }

    @Test
    void quotesInALabelAreEscapedInTheTemplates() {
        ObjectNode config = thermostat(c -> c.setHA_preset_modes("L'été=DAY"));

        Assertions.assertThat(texts(config.get("preset_modes"))).containsExactly("L'été");
        Assertions.assertThat(config.get("preset_mode_value_template").asText()).contains("{'DAY': 'L\\'été'}");
        Assertions.assertThat(config.get("preset_mode_command_template").asText()).contains("{'L\\'été': 'DAY'}");
    }

    @Test
    void noPresets() {
        ObjectNode config = thermostat(c -> c.setHA_preset_modes(""));

        Assertions.assertThat(config.has("preset_modes")).isFalse();
        Assertions.assertThat(config.has("preset_mode_command_topic")).isFalse();
        Assertions.assertThat(config.has("preset_mode_value_template")).isFalse();
    }

    @Test
    void noFanControlForARadiatorZone() {
        ObjectNode config = thermostat(c -> c.setHA_fan_modes(""));

        Assertions.assertThat(config.has("fan_modes")).isFalse();
        Assertions.assertThat(config.has("fan_mode_command_topic")).isFalse();
        Assertions.assertThat(config.has("fan_mode_state_template")).isFalse();
    }

    @Test
    void ownTemperatureRange() {
        ObjectNode config = thermostat(c -> {
            c.setHA_min_temp(5);
            c.setHA_max_temp(25.5);
        });

        Assertions.assertThat(config.get("min_temp").asDouble()).isEqualTo(5);
        Assertions.assertThat(config.get("max_temp").asDouble()).isEqualTo(25.5);
    }

    @Test
    void offStateIsReportedAsModeOffWithoutTarget() {
        ObjectNode config = thermostat(c -> c.setHA_hvac_action(true));

        Assertions.assertThat(config.get("mode_state_template").asText()).contains("value_json.state|upper == \"OFF\"");
        Assertions.assertThat(config.get("mode_command_template").asText()).contains("{\"state\": \"OFF\"}");
        Assertions.assertThat(config.get("temperature_state_template").asText()).startsWith("{% if value_json.state == 'OFF' %}None");
        Assertions.assertThat(config.get("action_topic").asText()).isEqualTo("~/state");
        Assertions.assertThat(config.get("action_template").asText()).contains("value_json.output_state");
    }

    @Test
    void bridgeAndSensorOneGetDifferentEntityIds() {
        CentralUnit centralUnit = new CentralUnit();
        centralUnit.setType(CentralUnitType.MICROS_PLUS);
        centralUnit.setVersion("test");
        ComponentSpec sensor1 = new ComponentSpec();
        sensor1.setNumber(1);
        sensor1.setDescription("Sensor 1");
        sensor1.setType("TEMPERATURE");
        Map<Function, List<ComponentSpec>> components = new LinkedHashMap<>();
        components.put(Function.SENSOR, List.of(sensor1));
        centralUnit.setComponentsTypes(components);

        String bridge = new HASensorConfig(new HAConfigParameters(new Teletask2MqttConfigurationProperties(), centralUnit, centralUnit.getBridge(), "teletask2mqtt/" + IDENTIFIER, null, IDENTIFIER, HomeAssistentAutoConfig.HADeviceType.SENSOR, null)).getConfig().get("default_entity_id").asText();
        String sensor = new HASensorConfig(new HAConfigParameters(new Teletask2MqttConfigurationProperties(), centralUnit, sensor1, "teletask2mqtt/" + IDENTIFIER, null, IDENTIFIER, HomeAssistentAutoConfig.HADeviceType.SENSOR, null)).getConfig().get("default_entity_id").asText();

        Assertions.assertThat(bridge).isEqualTo("sensor.teletask_MAN_TEST_localhost_bridge");
        Assertions.assertThat(sensor).isEqualTo("sensor.teletask-MAN_TEST_localhost-sensor-1");
    }
}
