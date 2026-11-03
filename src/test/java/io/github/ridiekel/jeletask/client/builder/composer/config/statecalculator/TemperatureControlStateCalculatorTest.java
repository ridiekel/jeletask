package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.component.TemperatureControlSpec;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;
import io.github.ridiekel.jeletask.client.spec.state.impl.TemperatureControlState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.HexFormat;
import java.util.List;

import static io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.TemperatureStateCalculator.bigDecimalToBytes;

class TemperatureControlStateCalculatorTest {
    private static byte[] event(int preset, int mode, int speed, boolean withNightCooling) {
        byte[] head = Bytes.concat(
                bigDecimalToBytes(new BigDecimal("20.5")),  // current
                bigDecimalToBytes(new BigDecimal("21.0")),  // target
                bigDecimalToBytes(new BigDecimal("21.0")),  // day preset
                bigDecimalToBytes(new BigDecimal("17.0")),  // night preset (heating)
                new byte[]{30, (byte) preset, (byte) mode, (byte) speed, (byte) 255, (byte) 255, 0, 0}
        );
        return withNightCooling ? Bytes.concat(head, bigDecimalToBytes(new BigDecimal("25.0"))) : head;
    }

    private static TemperatureControlSpec component() {
        TemperatureControlSpec component = new TemperatureControlSpec();
        component.setDecimals(1);
        return component;
    }

    @Test
    void knownValuesAreDecoded() {
        TemperatureControlState state = new TemperatureControlStateCalculator().fromEvent(component(), event(26, 95, 89, true));

        Assertions.assertThat(state.getCurrentTemperature()).isEqualByComparingTo("20.5");
        Assertions.assertThat(state.getTargetTemperature()).isEqualByComparingTo("21.0");
        Assertions.assertThat(state.getPreset()).isEqualTo(TemperatureControlStateCalculator.ValidTemperatureControlPreset.DAY);
        Assertions.assertThat(state.getMode()).isEqualTo(TemperatureControlStateCalculator.ValidTemperatureControlMode.HEAT);
        Assertions.assertThat(state.getFanspeed()).isEqualTo(TemperatureControlStateCalculator.ValidTemperatureControlSpeed.AUTO);
        Assertions.assertThat(state.getNightAtCoolingPresetTemperature()).isEqualByComparingTo("25.0");
    }

    @Test
    void unknownPresetAndSpeedDoNotThrow() {
        // Radiator thermostat without fan / preset: central reports 0 for these bytes ("No value for key '0'")
        TemperatureControlState state = new TemperatureControlStateCalculator().fromEvent(component(), event(0, 95, 0, true));

        Assertions.assertThat(state.getPreset()).isNull();
        Assertions.assertThat(state.getFanspeed()).isNull();
        Assertions.assertThat(state.getMode()).isEqualTo(TemperatureControlStateCalculator.ValidTemperatureControlMode.HEAT);
        Assertions.assertThat(state.getTargetTemperature()).isEqualByComparingTo("21.0");
    }

    @Test
    void seventeenByteEventWithoutNightCoolingPreset() {
        TemperatureControlState state = new TemperatureControlStateCalculator().fromEvent(component(), Bytes.concat(event(25, 95, 0, false), new byte[]{0}));

        Assertions.assertThat(state.getPreset()).isEqualTo(TemperatureControlStateCalculator.ValidTemperatureControlPreset.NIGHT);
        Assertions.assertThat(state.getNightAtCoolingPresetTemperature()).isNull();
    }

    @Test
    void targetTemperatureCommand() {
        TemperatureControlState state = new TemperatureControlState(OnOffToggleStateCalculator.ValidOnOffToggle.ON);
        state.setAction(TemperatureControlStateCalculator.ValidTemperatureControlAction.TARGET);
        state.setTargetTemperature(new BigDecimal("21.5"));

        byte[] command = new TemperatureControlStateCalculator().toCommand(component(), state);

        Assertions.assertThat(command).startsWith((byte) 87).hasSize(3);
    }

    private static ComponentSpec component(OnOffToggleStateCalculator.ValidOnOffToggle current) {
        ComponentSpec component = component();
        component.setState(new TemperatureControlState(current));
        return component;
    }

    private static List<byte[]> commands(ComponentSpec component, String message) {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();
        return c.toCommandStates(component, c.stateFromMessage(message)).stream().map(s -> c.toCommand(component, s)).toList();
    }

    @Test
    void turningOnSendsOn() {
        // HA power on payload
        Assertions.assertThat(commands(component(OnOffToggleStateCalculator.ValidOnOffToggle.OFF), "{ \"state\": \"ON\" }"))
                .containsExactly(new byte[]{(byte) 255});
    }

    @Test
    void turningOffSendsOff() {
        // HA mode_command_template for 'off'
        Assertions.assertThat(commands(component(OnOffToggleStateCalculator.ValidOnOffToggle.ON), "{\"state\": \"OFF\"}"))
                .containsExactly(new byte[]{0});
    }

    @Test
    void modePresetOrFanOnAnOffThermostatSendsOnFirst() {
        ComponentSpec off = component(OnOffToggleStateCalculator.ValidOnOffToggle.OFF);

        // HA mode_command_template for 'heat' while the thermostat is OFF: ON, then the mode itself
        Assertions.assertThat(commands(off, "{\"state\": \"ON\", \"mode\": \"HEAT\"}"))
                .containsExactly(new byte[]{(byte) 255}, new byte[]{95});
        Assertions.assertThat(commands(off, "{\"state\": \"ON\", \"preset\": \"NIGHT\"}"))
                .containsExactly(new byte[]{(byte) 255}, new byte[]{25});
        Assertions.assertThat(commands(off, "{\"state\": \"ON\", \"fanspeed\": \"LOW\"}"))
                .containsExactly(new byte[]{(byte) 255}, new byte[]{97});
    }

    @Test
    void modeOnAnOnThermostatIsSentAsIs() {
        Assertions.assertThat(commands(component(OnOffToggleStateCalculator.ValidOnOffToggle.ON), "{\"state\": \"ON\", \"mode\": \"HEAT\"}"))
                .containsExactly(new byte[]{95});
    }

    @Test
    void frostPresetCommand() {
        Assertions.assertThat(commands(component(OnOffToggleStateCalculator.ValidOnOffToggle.ON), "{\"state\": \"ON\", \"preset\": \"FROST\"}"))
                .containsExactly(new byte[]{24});
    }

    @Test
    void unknownModeDoesNotThrow() {
        TemperatureControlState state = new TemperatureControlStateCalculator().fromEvent(component(), event(26, 200, 89, true));  // 0 is mode OFF, 200 is not a mode

        Assertions.assertThat(state.getMode()).isNull();
        Assertions.assertThat(state.getPreset()).isEqualTo(TemperatureControlStateCalculator.ValidTemperatureControlPreset.DAY);
    }

    @Test
    void stateWithoutPresetModeOrFanRoundTripsThroughAnEvent() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();
        TemperatureControlState state = c.fromEvent(component(), event(0, 0, 0, true));

        TemperatureControlState again = c.fromEvent(component(), c.toEventForTesting(component(), state));

        Assertions.assertThat(again).isEqualTo(state);
    }

    @Test
    void manualPresetResendsCurrentTarget() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();
        ComponentSpec component = component();
        TemperatureControlState current = new TemperatureControlState(OnOffToggleStateCalculator.ValidOnOffToggle.ON);
        current.setTargetTemperature(new BigDecimal("19.0"));
        component.setState(current);

        byte[] manual = c.toCommand(component, c.stateFromMessage("{\"state\": \"ON\", \"preset\": \"MANUAL\"}"));

        TemperatureControlState target = new TemperatureControlState(OnOffToggleStateCalculator.ValidOnOffToggle.ON);
        target.setAction(TemperatureControlStateCalculator.ValidTemperatureControlAction.TARGET);
        target.setTargetTemperature(new BigDecimal("19.0"));
        Assertions.assertThat(manual).containsExactly(c.toCommand(component, target));
    }

    @Test
    void manualPresetWithItsOwnTarget() {
        Assertions.assertThat(commands(component(OnOffToggleStateCalculator.ValidOnOffToggle.ON), "{\"state\": \"ON\", \"preset\": \"MANUAL\", \"target_temperature\": 20.5}"))
                .containsExactly(Bytes.concat(new byte[]{87}, bigDecimalToBytes(new BigDecimal("20.5"))));
    }

    @Test
    void manualPresetWithoutAnyTargetIsRejected() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();
        ComponentSpec component = component(OnOffToggleStateCalculator.ValidOnOffToggle.ON);
        TemperatureControlState manual = c.stateFromMessage("{\"state\": \"ON\", \"preset\": \"MANUAL\"}");

        Assertions.assertThatThrownBy(() -> c.toCommand(component, manual))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target_temperature");
    }

    @Test
    void presetTemperatureCommands() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();

        // SET_TEMPSETDAY (29): (21.5 + 40) x 2 = 123
        Assertions.assertThat(c.toCommand(component(), c.stateFromMessage("{\"state\": \"ON\", \"action\": \"DAY_PRESET\", \"day_preset_temperature\": 21.5}")))
                .containsExactly((byte) 29, (byte) 123);
        // SET_TEMPSETNIGHT (27): (18 + 40) x 2 = 116
        Assertions.assertThat(c.toCommand(component(), c.stateFromMessage("{\"state\": \"ON\", \"action\": \"NIGHT_PRESET\", \"night_at_heating_preset_temperature\": 18}")))
                .containsExactly((byte) 27, (byte) 116);
        // SET_TEMPSETSTANDBY (88): eco offset in 0.5 °C steps, 2.5 x 2 = 5 (measured on a MICROS+)
        Assertions.assertThat(c.toCommand(component(), c.stateFromMessage("{\"state\": \"ON\", \"action\": \"ECO_OFFSET\", \"eco_preset_offset\": 2.5}")))
                .containsExactly((byte) 88, (byte) 5);
    }

    @Test
    void presetTemperatureCommandsDoNotTurnAnOffThermostatOn() {
        Assertions.assertThat(commands(component(OnOffToggleStateCalculator.ValidOnOffToggle.OFF), "{\"state\": \"ON\", \"action\": \"DAY_PRESET\", \"day_preset_temperature\": 20}"))
                .containsExactly(new byte[]{29, (byte) 120});
    }

    @Test
    void presetTemperatureCommandsAtTheEdgesOfTheByte() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();

        Assertions.assertThat(c.toCommand(component(), c.stateFromMessage("{\"state\": \"ON\", \"action\": \"DAY_PRESET\", \"day_preset_temperature\": -40}")))
                .containsExactly((byte) 29, (byte) 0);
        Assertions.assertThat(c.toCommand(component(), c.stateFromMessage("{\"state\": \"ON\", \"action\": \"NIGHT_PRESET\", \"night_at_heating_preset_temperature\": 87.5}")))
                .containsExactly((byte) 27, (byte) 255);
        Assertions.assertThat(c.toCommand(component(), c.stateFromMessage("{\"state\": \"ON\", \"action\": \"ECO_OFFSET\", \"eco_preset_offset\": 25.5}")))
                .containsExactly((byte) 88, (byte) 51);
    }

    @Test
    void presetTemperatureOutOfRangeIsRejected() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();
        ComponentSpec component = component();
        // (100 + 40) x 2 = 280 would wrap around to 24, which the central stores as -28 °C
        TemperatureControlState dayTooHigh = c.stateFromMessage("{\"state\": \"ON\", \"action\": \"DAY_PRESET\", \"day_preset_temperature\": 100}");
        TemperatureControlState nightTooLow = c.stateFromMessage("{\"state\": \"ON\", \"action\": \"NIGHT_PRESET\", \"night_at_heating_preset_temperature\": -40.5}");
        TemperatureControlState negativeEcoOffset = c.stateFromMessage("{\"state\": \"ON\", \"action\": \"ECO_OFFSET\", \"eco_preset_offset\": -2}");
        // 26 x 10 does not fit in the byte of a report
        TemperatureControlState ecoOffsetTooHigh = c.stateFromMessage("{\"state\": \"ON\", \"action\": \"ECO_OFFSET\", \"eco_preset_offset\": 26}");

        Assertions.assertThatThrownBy(() -> c.toCommand(component, dayTooHigh))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("day_preset_temperature");
        Assertions.assertThatThrownBy(() -> c.toCommand(component, nightTooLow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("night_at_heating_preset_temperature");
        Assertions.assertThatThrownBy(() -> c.toCommand(component, negativeEcoOffset))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eco_preset_offset");
        Assertions.assertThatThrownBy(() -> c.toCommand(component, ecoOffsetTooHigh))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eco_preset_offset");
    }

    @Test
    void actionWithoutItsValueIsRejected() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();
        ComponentSpec component = component();

        for (String action : List.of("TARGET", "DAY_PRESET", "NIGHT_PRESET", "ECO_OFFSET")) {
            TemperatureControlState withoutValue = c.stateFromMessage("{\"state\": \"ON\", \"action\": \"" + action + "\"}");

            Assertions.assertThatThrownBy(() -> c.toCommand(component, withoutValue))
                    .as(action)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageStartingWith("Missing");
        }
    }

    @Test
    void mockedCentralAppliesOnAndOff() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();
        ComponentSpec component = component(OnOffToggleStateCalculator.ValidOnOffToggle.OFF);

        Assertions.assertThat(c.fromCommandForTesting(component, new byte[]{(byte) 255}).getState()).isEqualTo(OnOffToggleStateCalculator.ValidOnOffToggle.ON);
        Assertions.assertThat(c.fromCommandForTesting(component, new byte[]{0}).getState()).isEqualTo(OnOffToggleStateCalculator.ValidOnOffToggle.OFF);
    }

    @Test
    void presetTemperatureCommandsRoundTrip() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();
        ComponentSpec component = component();
        component.setState(new TemperatureControlState(OnOffToggleStateCalculator.ValidOnOffToggle.ON));

        TemperatureControlState day = c.fromCommandForTesting(component, new byte[]{29, (byte) 123});
        Assertions.assertThat(day.getDayPresetTemperature()).isEqualByComparingTo("21.5");
        TemperatureControlState night = c.fromCommandForTesting(component, new byte[]{27, (byte) 116});
        Assertions.assertThat(night.getNightAtHeatingPresetTemperature()).isEqualByComparingTo("18.0");
        TemperatureControlState eco = c.fromCommandForTesting(component, new byte[]{88, 5});
        Assertions.assertThat(eco.getEcoPresetOffset()).isEqualByComparingTo("2.5");
    }

    @Test
    void ecoOffsetAbove12Point7IsNotNegative() {
        // byte 8 of the event is the eco offset x 10, unsigned (150 = 15.0 °C, not -10.6)
        byte[] e = event(26, 95, 0, true);
        e[8] = (byte) 150;
        TemperatureControlState state = new TemperatureControlStateCalculator().fromEvent(component(), e);
        Assertions.assertThat(state.getEcoPresetOffset()).isEqualByComparingTo("15.0");
    }

    @Test
    void reportedBytesAreUnsignedInTheJson() throws Exception {
        // MESSAGES.md documents window_open as 0 / 255, not -1
        byte[] e = event(26, 95, 0, true);
        e[13] = (byte) 255;
        e[14] = (byte) 137;
        e[15] = (byte) 128;

        JsonNode json = State.OBJECT_MAPPER.readTree(new TemperatureControlStateCalculator().fromEvent(component(), e).toString());

        Assertions.assertThat(json.get("window_open").asInt()).isEqualTo(255);
        Assertions.assertThat(json.get("output_state").asInt()).isEqualTo(137);
        Assertions.assertThat(json.get("swing_direction").asInt()).isEqualTo(128);
    }

    @Test
    void anEventShorterThanSeventeenBytesIsNotAState() {
        Assertions.assertThat(new TemperatureControlStateCalculator().fromEvent(component(), event(26, 95, 89, false))).isNull();
    }

    @Test
    void everyFieldOfAnEventIsReadFromItsOwnPosition() {
        // Layout of the TT15132 sensor event: value, target, day, night (2 bytes each), standby, preset, mode, speed, power, frost, not used, direction, night cooling (2 bytes)
        byte[] event = Bytes.concat(
                bigDecimalToBytes(new BigDecimal("20.5")),
                bigDecimalToBytes(new BigDecimal("21.0")),
                bigDecimalToBytes(new BigDecimal("22.0")),
                bigDecimalToBytes(new BigDecimal("17.0")),
                new byte[]{30, 26, 95, 89, 0, 7, 9, 2},
                bigDecimalToBytes(new BigDecimal("25.0"))
        );

        TemperatureControlState state = new TemperatureControlStateCalculator().fromEvent(component(), event);

        Assertions.assertThat(state.getCurrentTemperature()).isEqualByComparingTo("20.5");
        Assertions.assertThat(state.getTargetTemperature()).isEqualByComparingTo("21.0");
        Assertions.assertThat(state.getDayPresetTemperature()).isEqualByComparingTo("22.0");
        Assertions.assertThat(state.getNightAtHeatingPresetTemperature()).isEqualByComparingTo("17.0");
        Assertions.assertThat(state.getEcoPresetOffset()).isEqualByComparingTo("3.0");
        Assertions.assertThat(state.getPreset()).isEqualTo(TemperatureControlStateCalculator.ValidTemperatureControlPreset.DAY);
        Assertions.assertThat(state.getMode()).isEqualTo(TemperatureControlStateCalculator.ValidTemperatureControlMode.HEAT);
        Assertions.assertThat(state.getFanspeed()).isEqualTo(TemperatureControlStateCalculator.ValidTemperatureControlSpeed.AUTO);
        Assertions.assertThat(state.getState()).isEqualTo(OnOffToggleStateCalculator.ValidOnOffToggle.OFF);
        Assertions.assertThat(state.getWindowOpen()).isEqualTo(7);
        Assertions.assertThat(state.getOutputState()).isEqualTo(9);
        Assertions.assertThat(state.getSwingDirection()).isEqualTo(2);
        Assertions.assertThat(state.getNightAtCoolingPresetTemperature()).isEqualByComparingTo("25.0");
    }

    @Test
    void temperaturesBelowZeroAreDecoded() {
        byte[] e = event(26, 95, 89, true);
        // (-5.5 + 273) x 10 = 2675
        e[0] = 0x0A;
        e[1] = 0x73;

        Assertions.assertThat(new TemperatureControlStateCalculator().fromEvent(component(), e).getCurrentTemperature()).isEqualByComparingTo("-5.5");
    }

    @Test
    void temperaturesAreRoundedUpToTheDecimalsOfTheComponent() {
        TemperatureControlSpec noDecimals = component();
        noDecimals.setDecimals(0);

        Assertions.assertThat(new TemperatureControlStateCalculator().fromEvent(noDecimals, event(26, 95, 89, true)).getCurrentTemperature()).isEqualByComparingTo("21");
    }

    @ParameterizedTest
    @CsvSource({
            "preset,   FROST,  24",
            "preset,   DAY,    26",
            "preset,   NIGHT,  25",
            "preset,   ECO,    93",
            "mode,     AUTO,   94",
            "mode,     HEAT,   95",
            "mode,     COOL,   96",
            "mode,     VENT,   105",
            "fanspeed, LOW,    97",
            "fanspeed, MEDIUM, 98",
            "fanspeed, HIGH,   99",
            "fanspeed, AUTO,   89",
    })
    void settingsUseTheValuesOfTheManual(String field, String value, int setting) {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();

        Assertions.assertThat(c.toCommand(component(), c.stateFromMessage("{\"state\": \"ON\", \"" + field + "\": \"" + value + "\"}"))).containsExactly((byte) setting);

        TemperatureControlState reported = c.fromEvent(component(), event(field.equals("preset") ? setting : 0, field.equals("mode") ? setting : 0, field.equals("fanspeed") ? setting : 0, true));
        Assertions.assertThat(field(reported, field)).isEqualTo(value);

        TemperatureControlState applied = c.fromCommandForTesting(component(OnOffToggleStateCalculator.ValidOnOffToggle.ON), new byte[]{(byte) setting});
        Assertions.assertThat(field(applied, field)).isEqualTo(value);
    }

    private static String field(TemperatureControlState state, String field) {
        return String.valueOf(switch (field) {
            case "preset" -> state.getPreset();
            case "mode" -> state.getMode();
            case "fanspeed" -> state.getFanspeed();
            default -> throw new IllegalArgumentException(field);
        });
    }

    @Test
    void mockedCentralRejectsAnUnknownSetting() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();
        ComponentSpec component = component(OnOffToggleStateCalculator.ValidOnOffToggle.ON);
        byte[] unknownSetting = {(byte) 200};

        Assertions.assertThatThrownBy(() -> c.fromCommandForTesting(component, unknownSetting))
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // OFF wins from everything else
            "{\"state\": \"OFF\", \"action\": \"TARGET\", \"target_temperature\": 20, \"mode\": \"HEAT\"} | 00",
            // Action wins from mode: (20 + 40) x 2 = 120
            "{\"state\": \"ON\", \"action\": \"DAY_PRESET\", \"day_preset_temperature\": 20, \"mode\": \"HEAT\"} | 1D78",
            // Mode wins from fan speed and preset
            "{\"state\": \"ON\", \"mode\": \"HEAT\", \"fanspeed\": \"LOW\", \"preset\": \"DAY\"} | 5F",
            // Fan speed wins from preset
            "{\"state\": \"ON\", \"fanspeed\": \"LOW\", \"preset\": \"DAY\"} | 61",
            // Mode OFF is SET_OFF
            "{\"state\": \"ON\", \"mode\": \"OFF\"} | 00",
    })
    void onlyTheMostImportantPartOfAMessageIsSent(String message, String expected) {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();

        Assertions.assertThat(c.toCommand(component(), c.stateFromMessage(message))).containsExactly(HexFormat.of().parseHex(expected));
    }

    @Test
    void targetTemperatureIsSentHighByteFirst() {
        // TEMPMANUAL_TARGET: (21.5 + 273) x 10 = 2945 = 0x0B81
        Assertions.assertThat(commands(component(OnOffToggleStateCalculator.ValidOnOffToggle.ON), "{\"state\": \"ON\", \"action\": \"TARGET\", \"target_temperature\": 21.5}"))
                .containsExactly(new byte[]{87, 0x0B, (byte) 0x81});
    }

    @Test
    void mockedCentralAppliesATargetTemperature() {
        TemperatureControlState state = new TemperatureControlStateCalculator().fromCommandForTesting(component(OnOffToggleStateCalculator.ValidOnOffToggle.ON), new byte[]{87, 0x0B, (byte) 0x81});

        Assertions.assertThat(state.getTargetTemperature()).isEqualByComparingTo("21.5");
    }

    private static ComponentSpec componentWithState(State<?> state) {
        ComponentSpec component = component();
        component.setState(state);
        return component;
    }

    @Test
    void noOnFirstWithoutAKnownOffThermostat() {
        String heat = "{\"state\": \"ON\", \"mode\": \"HEAT\"}";

        Assertions.assertThat(commands(null, heat)).containsExactly(new byte[]{95});
        Assertions.assertThat(commands(component(), heat)).containsExactly(new byte[]{95});
        // Not a thermostat state, even though it says OFF
        Assertions.assertThat(commands(componentWithState(new OnOffState(OnOffToggleStateCalculator.ValidOnOffToggle.OFF)), heat)).containsExactly(new byte[]{95});
    }

    @Test
    void turningOffAnOffThermostatDoesNotSendOnFirst() {
        Assertions.assertThat(commands(component(OnOffToggleStateCalculator.ValidOnOffToggle.OFF), "{\"state\": \"OFF\", \"mode\": \"HEAT\"}"))
                .containsExactly(new byte[]{0});
    }

    @Test
    void manualPresetOnAnOffThermostatSendsOnAndThenTheStoredTarget() {
        TemperatureControlState off = new TemperatureControlState(OnOffToggleStateCalculator.ValidOnOffToggle.OFF);
        off.setTargetTemperature(new BigDecimal("19.0"));

        // (19 + 273) x 10 = 2920 = 0x0B68
        Assertions.assertThat(commands(componentWithState(off), "{\"state\": \"ON\", \"preset\": \"MANUAL\"}"))
                .containsExactly(new byte[]{(byte) 255}, new byte[]{87, 0x0B, 0x68});
    }

    @Test
    void manualPresetCannotTakeATargetFromAnythingButAThermostat() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();
        TemperatureControlState manual = c.stateFromMessage("{\"state\": \"ON\", \"preset\": \"MANUAL\"}");
        ComponentSpec notAThermostat = componentWithState(new OnOffState(OnOffToggleStateCalculator.ValidOnOffToggle.ON));

        Assertions.assertThatThrownBy(() -> c.toCommand(null, manual))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target_temperature");
        Assertions.assertThatThrownBy(() -> c.toCommand(notAThermostat, manual))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target_temperature");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "DAY_PRESET   | day_preset_temperature              | 21.2 | 1D | 122",
            "DAY_PRESET   | day_preset_temperature              | 21.25 | 1D | 123",
            "DAY_PRESET   | day_preset_temperature              | 21.3 | 1D | 123",
            "NIGHT_PRESET | night_at_heating_preset_temperature | -40  | 1B | 0",
            "DAY_PRESET   | day_preset_temperature              | 87.5 | 1D | 255",
            "ECO_OFFSET   | eco_preset_offset                   | 0    | 58 | 0",
            "ECO_OFFSET   | eco_preset_offset                   | 0.2  | 58 | 0",
            "ECO_OFFSET   | eco_preset_offset                   | 0.3  | 58 | 1",
    })
    void presetValuesAreRoundedToTheNearestStep(String action, String attribute, String value, String setting, int expected) {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();

        byte[] command = c.toCommand(component(), c.stateFromMessage("{\"state\": \"ON\", \"action\": \"" + action + "\", \"" + attribute + "\": " + value + "}"));

        Assertions.assertThat(command).containsExactly(HexFormat.of().parseHex(setting)[0], (byte) expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "DAY_PRESET   | day_preset_temperature              | 87.6",
            "DAY_PRESET   | day_preset_temperature              | -40.1",
            "NIGHT_PRESET | night_at_heating_preset_temperature | 87.6",
            "ECO_OFFSET   | eco_preset_offset                   | 25.6",
            "ECO_OFFSET   | eco_preset_offset                   | -0.1",
    })
    void presetValuesJustOutsideTheRangeAreRejected(String action, String attribute, String value) {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();
        TemperatureControlState state = c.stateFromMessage("{\"state\": \"ON\", \"action\": \"" + action + "\", \"" + attribute + "\": " + value + "}");
        ComponentSpec component = component();

        Assertions.assertThatThrownBy(() -> c.toCommand(component, state))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(attribute)
                .hasMessageContaining(value);
    }

    @Test
    void mockedCentralAppliesPresetBytesAtTheirEdges() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();
        ComponentSpec component = component(OnOffToggleStateCalculator.ValidOnOffToggle.ON);

        Assertions.assertThat(c.fromCommandForTesting(component, new byte[]{29, 0}).getDayPresetTemperature()).isEqualByComparingTo("-40.0");
        Assertions.assertThat(c.fromCommandForTesting(component, new byte[]{27, (byte) 255}).getNightAtHeatingPresetTemperature()).isEqualByComparingTo("87.5");
        Assertions.assertThat(c.fromCommandForTesting(component, new byte[]{88, 51}).getEcoPresetOffset()).isEqualByComparingTo("25.5");
    }

    @Test
    void aFullStateSurvivesAnEventWithTheHighestValues() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();
        TemperatureControlState state = new TemperatureControlState(OnOffToggleStateCalculator.ValidOnOffToggle.ON);
        state.setCurrentTemperature(new BigDecimal("20.5"));
        state.setTargetTemperature(new BigDecimal("21.0"));
        state.setDayPresetTemperature(new BigDecimal("22.5"));
        state.setNightAtHeatingPresetTemperature(new BigDecimal("17.0"));
        state.setNightAtCoolingPresetTemperature(new BigDecimal("25.0"));
        // 25.5 x 10 = 255: the highest offset a report can hold
        state.setEcoPresetOffset(new BigDecimal("25.5"));
        state.setPreset(TemperatureControlStateCalculator.ValidTemperatureControlPreset.ECO);
        state.setMode(TemperatureControlStateCalculator.ValidTemperatureControlMode.COOL);
        state.setFanspeed(TemperatureControlStateCalculator.ValidTemperatureControlSpeed.HIGH);
        state.setWindowOpen(255);
        state.setOutputState(137);
        state.setSwingDirection(128);

        Assertions.assertThat(c.fromEvent(component(), c.toEventForTesting(component(), state))).isEqualTo(state);
    }

    @Test
    void manualIsReportedAsNoPreset() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();
        TemperatureControlState state = c.fromEvent(component(), event(26, 95, 89, true));
        state.setPreset(TemperatureControlStateCalculator.ValidTemperatureControlPreset.MANUAL);

        Assertions.assertThat(c.fromEvent(component(), c.toEventForTesting(component(), state)).getPreset()).isNull();
    }

    @Test
    void homeAssistantPayloadsToCommand() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();

        // preset_mode_command_template
        Assertions.assertThat(c.toCommand(component(), c.stateFromMessage("{\"state\": \"ON\", \"preset\": \"NIGHT\"}")))
                .containsExactly((byte) 25);
        // temperature_command_template
        Assertions.assertThat(c.toCommand(component(), c.stateFromMessage("{ \"state\": \"ON\", \"action\": \"TARGET\", \"target_temperature\": 21.5 }")))
                .startsWith((byte) 87).hasSize(3);
    }
}
