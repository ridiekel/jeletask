package io.github.ridiekel.jeletask.mqtt.listener.homeassistant;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.CentralUnitType;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.TestComponents;
import io.github.ridiekel.jeletask.client.spec.component.OnOffSpec;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

class HomeAssistentAutoConfigTest {
    private static HAConfig<?> config(Function function, Consumer<ComponentSpec> configure) {
        ComponentSpec component = TestComponents.of(function, 35);
        component.setDescription("Valve");
        configure.accept(component);

        CentralUnit centralUnit = new CentralUnit();
        centralUnit.setType(CentralUnitType.MICROS_PLUS);
        centralUnit.setVersion("test");
        Map<Function, List<ComponentSpec>> components = new LinkedHashMap<>();
        components.put(function, List.of(component));
        centralUnit.setComponentsTypes(components);
        centralUnit.getAllComponents();

        Teletask2MqttConfigurationProperties configuration = new Teletask2MqttConfigurationProperties();
        configuration.setMqtt(new Teletask2MqttConfigurationProperties.MqttConfiguration());
        return new HomeAssistentAutoConfig(configuration, centralUnit, "teletask2mqtt/1", "1").toConfig(component);
    }

    private static void assertReadOnlyBinarySensor(HAConfig<?> config, Function function) {
        ObjectNode node = config.getConfig();

        Assertions.assertThat(config.getMqttConfigTopic().topic()).isEqualTo("homeassistant/binary_sensor/1/" + function.toString().toLowerCase() + "_35/config");
        Assertions.assertThat(node.get("payload_on").asText()).isEqualTo("ON");
        Assertions.assertThat(node.get("payload_off").asText()).isEqualTo("OFF");
        Assertions.assertThat(node.get("value_template").asText()).isEqualTo("{{ value_json.state }}");
        Assertions.assertThat(node.has("command_topic")).isFalse();
        Assertions.assertThat(node.has("state_on")).isFalse();
    }

    @ParameterizedTest
    @EnumSource(names = {"RELAY", "FLAG", "GENMOOD", "LOCMOOD", "TIMEDMOOD", "TIMEDFNC"})
    void onOffFunctionWithTypeBinarySensor(Function function) {
        assertReadOnlyBinarySensor(config(function, c -> ((OnOffSpec) c).setType("BINARY_SENSOR")), function);
    }

    @ParameterizedTest
    @EnumSource(names = {"RELAY", "FLAG", "GENMOOD", "LOCMOOD", "TIMEDMOOD", "TIMEDFNC"})
    void onOffFunctionWithHaTypeBinarySensor(Function function) {
        assertReadOnlyBinarySensor(config(function, c -> c.setHaType("binary_sensor")), function);
    }

    @ParameterizedTest
    @EnumSource(names = {"RELAY", "FLAG", "GENMOOD", "LOCMOOD", "TIMEDMOOD", "TIMEDFNC"})
    void onOffFunctionThatCanBeSwitchedKeepsItsCommandPayloads(Function function) {
        ObjectNode node = config(function, c -> ((OnOffSpec) c).setType("switch")).getConfig();

        Assertions.assertThat(node.get("payload_on").asText()).contains("\"state\": \"ON\"");
        Assertions.assertThat(node.get("state_on").asText()).isEqualTo("ON");
        Assertions.assertThat(node.get("command_topic").asText()).isEqualTo("~/set");
    }
}
