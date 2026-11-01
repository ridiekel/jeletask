package io.github.ridiekel.jeletask.end2end;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.TemperatureControlStateCalculator;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.impl.TemperatureControlState;
import io.github.ridiekel.jeletask.mockserver.TeletaskMockServer;
import io.github.ridiekel.jeletask.mqtt.container.ha.Entity;
import io.github.ridiekel.jeletask.mqtt.container.ha.HomeAssistantContainer;
import io.github.ridiekel.jeletask.mqtt.container.mqtt.MatchTest;
import io.github.ridiekel.jeletask.utilities.StringUtilities;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@SuppressWarnings("resource")
class End2EndTemperatureControlTest extends MockingTeletaskTestSupport {
    private static final int NUMBER = 33;
    private static final String ENTITY = HomeAssistantContainer.getId(Function.SENSOR, NUMBER, "climate");
    private static final String SET_TOPIC = "test_prefix_teletask2mqtt/MAN_TEST_localhost/sensor/" + NUMBER + "/set";

    @Autowired
    private TeletaskMockServer teletaskServer;

    // A radiator zone as a MICROS+ reports it: heating only, no fan
    private static TemperatureControlState.TemperatureControlStateBuilder<?, ?> radiatorZone() {
        return TemperatureControlState.builder()
                .state(OnOffToggleStateCalculator.ValidOnOffToggle.ON)
                .currentTemperature(new BigDecimal("20.5"))
                .targetTemperature(new BigDecimal("21.0"))
                .dayPresetTemperature(new BigDecimal("21.0"))
                .nightAtHeatingPresetTemperature(new BigDecimal("17.0"))
                .nightAtCoolingPresetTemperature(new BigDecimal("25.0"))
                .ecoPresetOffset(new BigDecimal("3.0"))
                .preset(TemperatureControlStateCalculator.ValidTemperatureControlPreset.DAY)
                .mode(TemperatureControlStateCalculator.ValidTemperatureControlMode.HEAT)
                .windowOpen(255)
                .outputState(8)
                .swingDirection(0);
    }

    private void atTheWallPanel(TemperatureControlState state) {
        teletaskServer.sendEvent(Function.SENSOR, NUMBER, state);
    }

    private void stateShould(String describe, Predicate<JsonNode> test) {
        mqtt().expect().sensor(NUMBER).lastStateMessage().toHave().state().match(describe, capture -> {
            JsonNode json;
            try {
                json = StringUtilities.OBJECT_MAPPER.readTree(capture.getMessage());
            } catch (Exception e) {
                return new MatchTest.MatchTestResult(false, capture.getMessage());
            }
            return new MatchTest.MatchTestResult(test.test(json), capture.getMessage());
        });
    }

    private void haShould(String describe, Predicate<Entity> test) {
        Awaitility.await(describe)
                .pollInterval(250, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> ha().state(ENTITY), test);
    }

    private static boolean number(Object actual, String expected) {
        return actual instanceof Number n && new BigDecimal(n.toString()).compareTo(new BigDecimal(expected)) == 0;
    }

    private static boolean number(JsonNode actual, String expected) {
        return actual != null && actual.isNumber() && actual.decimalValue().compareTo(new BigDecimal(expected)) == 0;
    }

    // HA adds its own "none" to the presets of an MQTT climate
    private static List<?> withoutNone(Object presets) {
        return presets instanceof List<?> list ? list.stream().filter(p -> !Objects.equals(p, "none")).toList() : List.of();
    }

    private void callClimateService(String service, Map<String, Object> data) {
        Map<String, Object> body = new LinkedHashMap<>(data);
        body.put("entity_id", ENTITY);
        ha().callService("climate", service, body);
    }

    @Test
    void discovery() {
        haShould("presets, fan modes and temperature range", e ->
                Objects.equals(withoutNone(e.getAttributes().get("preset_modes")), List.of("day", "night", "eco", "frost"))
                        && Objects.equals(e.getAttributes().get("fan_modes"), List.of("auto", "low", "medium", "high"))
                        && number(e.getAttributes().get("min_temp"), "10")
                        && number(e.getAttributes().get("max_temp"), "30"));
    }

    @Test
    void radiatorZoneWithoutPresetAndFanIsPublished() {
        // Used to abort the whole state message with "No value for key '0'"
        atTheWallPanel(radiatorZone().preset(null).fanspeed(null).outputState(9).build());

        stateShould("mode HEAT, no preset and no fanspeed", s ->
                s.path("mode").asText().equals("HEAT") && !s.has("preset") && !s.has("fanspeed"));
        haShould("heat, heating, no preset", e ->
                Objects.equals(e.getState(), "heat")
                        && Objects.equals(e.getAttributes().get("hvac_action"), "heating")
                        && (e.getAttributes().get("preset_mode") == null || Objects.equals(e.getAttributes().get("preset_mode"), "none")));
    }

    @Test
    void idleThermostatWithAPreset() {
        atTheWallPanel(radiatorZone().outputState(8).build());

        haShould("heat, idle, preset day, target 21", e ->
                Objects.equals(e.getState(), "heat")
                        && Objects.equals(e.getAttributes().get("hvac_action"), "idle")
                        && Objects.equals(e.getAttributes().get("preset_mode"), "day")
                        && number(e.getAttributes().get("temperature"), "21.0"));
    }

    @Test
    void offThermostatIsOffWithoutTarget() {
        // The central keeps reporting the last mode and its frost target while OFF
        atTheWallPanel(radiatorZone().state(OnOffToggleStateCalculator.ValidOnOffToggle.OFF).targetTemperature(new BigDecimal("5.0")).build());

        haShould("off, no target", e ->
                Objects.equals(e.getState(), "off")
                        && Objects.equals(e.getAttributes().get("hvac_action"), "off")
                        && e.getAttributes().get("temperature") == null);
    }

    @Test
    void choosingAPresetInHomeAssistant() {
        atTheWallPanel(radiatorZone().build());
        haShould("preset day", e -> Objects.equals(e.getAttributes().get("preset_mode"), "day"));

        callClimateService("set_preset_mode", Map.of("preset_mode", "night"));

        stateShould("preset NIGHT", s -> s.path("preset").asText().equals("NIGHT"));
        haShould("preset night", e -> Objects.equals(e.getAttributes().get("preset_mode"), "night"));
    }

    @Test
    void choosingAModeOnAnOffThermostatTurnsItOnInThatMode() {
        atTheWallPanel(radiatorZone().state(OnOffToggleStateCalculator.ValidOnOffToggle.OFF).mode(TemperatureControlStateCalculator.ValidTemperatureControlMode.AUTO).build());
        haShould("off", e -> Objects.equals(e.getState(), "off"));

        callClimateService("set_hvac_mode", Map.of("hvac_mode", "heat"));

        stateShould("ON in mode HEAT", s -> s.path("state").asText().equals("ON") && s.path("mode").asText().equals("HEAT"));
        haShould("heat", e -> Objects.equals(e.getState(), "heat"));
    }

    @Test
    void turningOffInHomeAssistant() {
        atTheWallPanel(radiatorZone().build());
        haShould("heat", e -> Objects.equals(e.getState(), "heat"));

        callClimateService("set_hvac_mode", Map.of("hvac_mode", "off"));

        stateShould("OFF", s -> s.path("state").asText().equals("OFF"));
        haShould("off", e -> Objects.equals(e.getState(), "off"));
    }

    @Test
    void settingTheTargetInHomeAssistant() {
        atTheWallPanel(radiatorZone().build());
        haShould("target 21", e -> number(e.getAttributes().get("temperature"), "21.0"));

        callClimateService("set_temperature", Map.of("temperature", 22.5));

        stateShould("target 22.5", s -> number(s.path("target_temperature"), "22.5"));
        haShould("target 22.5", e -> number(e.getAttributes().get("temperature"), "22.5"));
    }

    @Test
    void presetTemperaturesOverMqtt() {
        atTheWallPanel(radiatorZone().build());
        stateShould("day preset 21", s -> number(s.path("day_preset_temperature"), "21.0"));

        mqtt().publish(SET_TOPIC, "{\"state\":\"ON\",\"action\":\"DAY_PRESET\",\"day_preset_temperature\":22}");
        stateShould("day preset 22", s -> number(s.path("day_preset_temperature"), "22.0"));

        mqtt().publish(SET_TOPIC, "{\"state\":\"ON\",\"action\":\"NIGHT_PRESET\",\"night_at_heating_preset_temperature\":16.5}");
        stateShould("night preset 16.5", s -> number(s.path("night_at_heating_preset_temperature"), "16.5"));

        mqtt().publish(SET_TOPIC, "{\"state\":\"ON\",\"action\":\"ECO_OFFSET\",\"eco_preset_offset\":2.5}");
        stateShould("eco offset 2.5", s -> number(s.path("eco_preset_offset"), "2.5"));
    }

    @Test
    void anOutOfRangePresetTemperatureIsNotSent() {
        atTheWallPanel(radiatorZone().build());
        stateShould("day preset 21", s -> number(s.path("day_preset_temperature"), "21.0"));

        // (100 + 40) x 2 does not fit in a byte: it used to arrive as -28 °C
        mqtt().publish(SET_TOPIC, "{\"state\":\"ON\",\"action\":\"DAY_PRESET\",\"day_preset_temperature\":100}");
        mqtt().publish(SET_TOPIC, "{\"state\":\"ON\",\"action\":\"ECO_OFFSET\",\"eco_preset_offset\":2.5}");

        stateShould("eco offset 2.5 and the day preset untouched", s ->
                number(s.path("eco_preset_offset"), "2.5") && number(s.path("day_preset_temperature"), "21.0"));
    }
}
