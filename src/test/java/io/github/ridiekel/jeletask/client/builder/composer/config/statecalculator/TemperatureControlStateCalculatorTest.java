package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.TemperatureControlState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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

    private static ComponentSpec component() {
        ComponentSpec component = new ComponentSpec();
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

        Assertions.assertThat(command).startsWith((byte) 87);
        Assertions.assertThat(command).hasSize(3);
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

        Assertions.assertThatThrownBy(() -> c.toCommand(component(OnOffToggleStateCalculator.ValidOnOffToggle.ON), c.stateFromMessage("{\"state\": \"ON\", \"preset\": \"MANUAL\"}")))
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

        // (100 + 40) x 2 = 280 would wrap around to 24, which the central stores as -28 °C
        Assertions.assertThatThrownBy(() -> c.toCommand(component(), c.stateFromMessage("{\"state\": \"ON\", \"action\": \"DAY_PRESET\", \"day_preset_temperature\": 100}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("day_preset_temperature");
        Assertions.assertThatThrownBy(() -> c.toCommand(component(), c.stateFromMessage("{\"state\": \"ON\", \"action\": \"NIGHT_PRESET\", \"night_at_heating_preset_temperature\": -40.5}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("night_at_heating_preset_temperature");
        Assertions.assertThatThrownBy(() -> c.toCommand(component(), c.stateFromMessage("{\"state\": \"ON\", \"action\": \"ECO_OFFSET\", \"eco_preset_offset\": -2}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eco_preset_offset");
        // 26 x 10 does not fit in the byte of a report
        Assertions.assertThatThrownBy(() -> c.toCommand(component(), c.stateFromMessage("{\"state\": \"ON\", \"action\": \"ECO_OFFSET\", \"eco_preset_offset\": 26}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eco_preset_offset");
    }

    @Test
    void actionWithoutItsValueIsRejected() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();

        for (String action : List.of("TARGET", "DAY_PRESET", "NIGHT_PRESET", "ECO_OFFSET")) {
            Assertions.assertThatThrownBy(() -> c.toCommand(component(), c.stateFromMessage("{\"state\": \"ON\", \"action\": \"" + action + "\"}")))
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
