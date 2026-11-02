package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.mapper.Mapper;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;
import io.github.ridiekel.jeletask.client.spec.state.impl.TemperatureControlState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.apache.commons.lang3.function.TriConsumer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter.UNSIGNED_BYTE;
import static io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.TemperatureStateCalculator.bigDecimalToBytes;
import static io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.TemperatureStateCalculator.bytesToBigDecimal;

/**
 * <pre>
 * +-----------------------------+-------+--------------------------------------------------------------------------+
 * | Constant                    | Value | Description                                                              |
 * +-----------------------------+-------+--------------------------------------------------------------------------+
 * | SET_TEMPUP                  | 21    | Increase the Temperature target with 0.5°C                               |
 * | SET_TEMPDOWN                | 22    | Decrease the Temperature target with 0.5°C                               |
 * +-----------------------------+-------+--------------------------------------------------------------------------+
 * | SET_TEMPFROST               | 24    | Set the system to frost protection                                       |
 * | SET_TEMPDAY                 | 26    | Set the preset to DAY                                                    |
 * | SET_TEMPNIGHT               | 25    | Set the preset to NIGHT                                                  |
 * | SET_TEMPSTANDBY             | 93    | Set the preset to STANDBY                                                |
 * +-----------------------------+-------+--------------------------------------------------------------------------+
 * | SET_TEMPSETDAY              | 29    | Set the Day preset temperature                                           |
 * | SET_TEMPSETSTANDBY          | 88    | Set the Standby preset temperature                                       |
 * | SET_TEMPSETNIGHT            | 27    | Set the Night preset temperature at heating                              |
 * | SET_TEMPSETNIGHTCOOL        | 56    | Set the Night preset temperature at cooling                              |
 * +-----------------------------+-------+--------------------------------------------------------------------------+
 * | SET_TEMPSPEED               | 31    | Scroll through the different speeds                                      |
 * | SET_TEMPSPLOW               | 97    | Set speed to Low                                                         |
 * | SET_TEMPSPMED               | 98    | Set speed to Medium                                                      |
 * | SET_TEMPSPHIGH              | 99    | Set speed to High                                                        |
 * | SET_TEMPSPAUTO              | 89    | Set speed to Auto                                                        |
 * +-----------------------------+-------+--------------------------------------------------------------------------+
 * | SET_TEMPMODE                | 30    | Scroll through the different modes                                       |
 * | SET_TEMPAUTO                | 94    | Set mode to Auto                                                         |
 * | SET_TEMPHEAT                | 95    | Set mode to Heat                                                         |
 * | SET_TEMPCOOL                | 96    | Set mode to Cool                                                         |
 * | SET_TEMPVENT                | 105   | Set mode to Vent, Airzone only                                           |
 * | SET_TEMPSTOP                | 106   | Set mode to Stop, Airzone only                                           |
 * | SET_TEMPHEATP               | 107   | Set mode to Heat+, Airzone only                                          |
 * +-----------------------------+-------+--------------------------------------------------------------------------+
 * | SET_TEMPONOFF               | 104   | Toggle between ON and OFF                                                |
 * +-----------------------------+-------+--------------------------------------------------------------------------+
 *
 * </pre>
 */
public class TemperatureControlStateCalculator extends StateCalculatorSupport<TemperatureControlState> {
    public static final OnOffToggleStateCalculator ON_OFF_TOGGLE_STATE_CALCULATOR = new OnOffToggleStateCalculator();
    private static final Mapper<ValidTemperatureControlAction> ACTION_MAPPER = new Mapper<>(ValidTemperatureControlAction.class, UNSIGNED_BYTE)
            .add(ValidTemperatureControlAction.TARGET, 87)
            .add(ValidTemperatureControlAction.DAY_PRESET, 29)
            .add(ValidTemperatureControlAction.NIGHT_PRESET, 27)
            .add(ValidTemperatureControlAction.ECO_OFFSET, 88);
    private static final Mapper<ValidTemperatureControlMode> MODE_MAPPER = new Mapper<>(ValidTemperatureControlMode.class, UNSIGNED_BYTE)
            .add(ValidTemperatureControlMode.OFF, 0)
            .add(ValidTemperatureControlMode.AUTO, 94)
            .add(ValidTemperatureControlMode.HEAT, 95)
            .add(ValidTemperatureControlMode.COOL, 96)
            .add(ValidTemperatureControlMode.VENT, 105)
            .add(ValidTemperatureControlMode.DRY, 108);
    private static final Mapper<ValidTemperatureControlSpeed> SPEED_MAPPER = new Mapper<>(ValidTemperatureControlSpeed.class, UNSIGNED_BYTE)
            .add(ValidTemperatureControlSpeed.LOW, 97)
            .add(ValidTemperatureControlSpeed.MEDIUM, 98)
            .add(ValidTemperatureControlSpeed.HIGH, 99)
            .add(ValidTemperatureControlSpeed.AUTO, 89);
    private static final Mapper<ValidTemperatureControlPreset> PRESET_MAPPER = new Mapper<>(ValidTemperatureControlPreset.class, UNSIGNED_BYTE)
            .add(ValidTemperatureControlPreset.FROST, 24)
            .add(ValidTemperatureControlPreset.DAY, 26)
            .add(ValidTemperatureControlPreset.NIGHT, 25)
            .add(ValidTemperatureControlPreset.ECO, 93);

    @Override
    public TemperatureControlState fromEvent(ComponentSpec component, byte[] dataBytes) {
        if (dataBytes.length < 17)
            return null;

        TemperatureControlState state = new TemperatureControlState(ON_OFF_TOGGLE_STATE_CALCULATOR.fromEvent(component, new byte[]{dataBytes[12]}).getState());
        state.setCurrentTemperature(bytesToBigDecimal(component, new byte[]{dataBytes[0], dataBytes[1]}));
        state.setTargetTemperature(bytesToBigDecimal(component, new byte[]{dataBytes[2], dataBytes[3]}));
        state.setDayPresetTemperature(bytesToBigDecimal(component, new byte[]{dataBytes[4], dataBytes[5]}));
        state.setNightAtHeatingPresetTemperature(bytesToBigDecimal(component, new byte[]{dataBytes[6], dataBytes[7]}));
        state.setEcoPresetOffset(new BigDecimal(Byte.toUnsignedInt(dataBytes[8])).divide(BigDecimal.TEN, 1, RoundingMode.HALF_UP));
        // Lenient: thermostats without e.g. a fan or preset report 0 here, which is not a valid setting value.
        state.setPreset(PRESET_MAPPER.toEnumOrNull(new byte[]{dataBytes[9]}));
        state.setMode(MODE_MAPPER.toEnumOrNull(new byte[]{dataBytes[10]}));
        state.setFanspeed(SPEED_MAPPER.toEnumOrNull(new byte[]{dataBytes[11]}));
        state.setWindowOpen(Byte.toUnsignedInt(dataBytes[13]));       // 0 = open, 255 = closed
        state.setOutputState(Byte.toUnsignedInt(dataBytes[14]));      // Untested, no idea about the values
        state.setSwingDirection(Byte.toUnsignedInt(dataBytes[15]));   // Untested, no idea about the values
        if (dataBytes.length >= 18) {
            state.setNightAtCoolingPresetTemperature(bytesToBigDecimal(component, new byte[]{dataBytes[16], dataBytes[17]}));
        }

        return state;
    }

    /**
     * +------------------------+---------------------------------------------------------------+
     * | Setting                | Data Parameters                                               |
     * +------------------------+---------------------------------------------------------------+
     * | SET_TEMPUP             | /                                                             |
     * | SET_TEMPDOWN           | /                                                             |
     * +------------------------+---------------------------------------------------------------+
     * | SET_TEMPFROST          | /                                                             |
     * | SET_TEMPNIGHT          | /                                                             |
     * | SET_TEMPSTANDBY        | /                                                             |
     * | SET_TEMPDAY            | /                                                             |
     * +------------------------+---------------------------------------------------------------+
     * | SET_TEMPSETNIGHT       | 1 byte: The new night preset for heating                      |
     * | SET_TEMPSETSTANDBY     | 1 byte: The new standby preset                                |
     * | SET_TEMPSETDAY         | 1 byte: The new day preset                                    |
     * | SET_TEMPSETNIGHTCOOL   | 1 byte: The new night preset for cooling                      |
     * +------------------------+---------------------------------------------------------------+
     * | SET_TEMPMODE           | / (used to scroll through the "modes")                        |
     * | SET_TEMPAUTO           | /                                                             |
     * | SET_TEMPHEAT           | /                                                             |
     * | SET_TEMPCOOL           | /                                                             |
     * | SET_TEMPVENT           | / (only for Airzone sensors)                                  |
     * | SET_TEMPSTOP           | / (only for Airzone sensors)                                  |
     * | SET_TEMPHEATP          | / (only for Airzone sensors)                                  |
     * +------------------------+---------------------------------------------------------------+
     * | SET_TEMPSPEED          | / (used to scroll through the speeds)                         |
     * | SET_TEMPSPLOW          | /                                                             |
     * | SET_TEMPSPMED          | /                                                             |
     * | SET_TEMPSPHIGH         | /                                                             |
     * | SET_TEMPSPAUTO         | /                                                             |
     * +------------------------+---------------------------------------------------------------+
     * | SET_ON                 | /                                                             |
     * | SET_OFF                | /                                                             |
     * | SET_TEMPONOFF          | /                                                             |
     * +------------------------+---------------------------------------------------------------+
     * | TEMPMANUAL_TARGET      | Short as value: byte[0]=Higher byte, byte[1]=lower byte       |
     * +------------------------+---------------------------------------------------------------+
     *
     * @param component
     * @param state     The state that needs to be converted to a command
     * @return The byte[] representation of the state
     */
    @Override
    public byte[] toCommand(ComponentSpec component, TemperatureControlState state) {
        byte[] setting = new byte[]{};
        byte[] data = new byte[]{};

        if (state.getState() == OnOffToggleStateCalculator.ValidOnOffToggle.OFF) {
            setting = ON_OFF_TOGGLE_STATE_CALCULATOR.toCommand(component, new OnOffState(OnOffToggleStateCalculator.ValidOnOffToggle.OFF));
        } else if (state.getAction() == null && state.getMode() == null && state.getFanspeed() == null && state.getPreset() == null) {
            // Just turning on: the central restores the previous mode and target temperature
            setting = ON_OFF_TOGGLE_STATE_CALCULATOR.toCommand(component, new OnOffState(OnOffToggleStateCalculator.ValidOnOffToggle.ON));
        } else if (state.getAction() != null) {
            setting = ACTION_MAPPER.toBytes(state.getAction());
            data = state.getAction().getData(state);
        } else if (state.getMode() != null) {
            setting = MODE_MAPPER.toBytes(state.getMode());
        } else if (state.getFanspeed() != null) {
            setting = SPEED_MAPPER.toBytes(state.getFanspeed());
        } else if (state.getPreset() == ValidTemperatureControlPreset.MANUAL) {
            // There is no "manual" preset in Teletask: re-sending the target temperature switches the zone to manual
            TemperatureControlState target = new TemperatureControlState(state.getState());
            target.setTargetTemperature(Optional.ofNullable(state.getTargetTemperature()).orElseGet(() -> currentTargetTemperature(component)));
            setting = ACTION_MAPPER.toBytes(ValidTemperatureControlAction.TARGET);
            data = ValidTemperatureControlAction.TARGET.getData(target);
        } else if (state.getPreset() != null) {
            setting = PRESET_MAPPER.toBytes(state.getPreset());
        }

        return Bytes.concat(setting, data);
    }

    /**
     * A mode, fan speed or preset command does not switch an OFF thermostat back on, so send ON first.
     */
    @Override
    public List<TemperatureControlState> toCommandStates(ComponentSpec component, TemperatureControlState state) {
        if (isOff(component)
                && state.getState() == OnOffToggleStateCalculator.ValidOnOffToggle.ON
                && state.getAction() == null
                && (state.getMode() != null || state.getFanspeed() != null || state.getPreset() != null)) {
            return List.of(new TemperatureControlState(OnOffToggleStateCalculator.ValidOnOffToggle.ON), state);
        }
        return List.of(state);
    }

    private static BigDecimal currentTargetTemperature(ComponentSpec component) {
        return component != null && component.getState() instanceof TemperatureControlState current ? current.getTargetTemperature() : null;
    }

    private static boolean isOff(ComponentSpec component) {
        return component != null
                && component.getState() instanceof TemperatureControlState current
                && current.getState() == OnOffToggleStateCalculator.ValidOnOffToggle.OFF;
    }

    @Override
    public byte[] toEventForTesting(ComponentSpec component, TemperatureControlState state) {
        return Bytes.concat(
                bigDecimalToBytes(state.getCurrentTemperature()),
                bigDecimalToBytes(state.getTargetTemperature()),
                bigDecimalToBytes(state.getDayPresetTemperature()),
                bigDecimalToBytes(state.getNightAtHeatingPresetTemperature()),
                UNSIGNED_BYTE.convert(state.getEcoPresetOffset().multiply(BigDecimal.TEN).setScale(0, RoundingMode.HALF_UP)),
                // No active preset, mode or fan speed is reported as 0 (TDS15132: "SET_OFF if there is no active preset")
                state.getPreset() == null || state.getPreset() == ValidTemperatureControlPreset.MANUAL ? new byte[]{0} : PRESET_MAPPER.toBytes(state.getPreset()),
                state.getMode() == null ? new byte[]{0} : MODE_MAPPER.toBytes(state.getMode()),
                state.getFanspeed() == null ? new byte[]{0} : SPEED_MAPPER.toBytes(state.getFanspeed()),
                ON_OFF_TOGGLE_STATE_CALCULATOR.toCommand(component, new OnOffState(state.getState())),
                UNSIGNED_BYTE.convert(state.getWindowOpen()),
                UNSIGNED_BYTE.convert(state.getOutputState()),
                UNSIGNED_BYTE.convert(state.getSwingDirection()),
                bigDecimalToBytes(state.getNightAtCoolingPresetTemperature())
        );
    }

    @Override
    public TemperatureControlState fromCommandForTesting(ComponentSpec component, byte[] dataBytes) {
        TemperatureControlState state = (TemperatureControlState) component.getState();

        int setting = Byte.toUnsignedInt(dataBytes[0]);
        if (setting == 255 || setting == 0) {
            // SET_ON / SET_OFF
            state.setState(ON_OFF_TOGGLE_STATE_CALCULATOR.fromEvent(component, new byte[]{dataBytes[0]}).getState());
            return state;
        }

        //Could be done better I guess
        try {
            ValidTemperatureControlAction action = ACTION_MAPPER.toEnum(new byte[]{dataBytes[0]});
            action.applyData(component, state, dataBytes);
        } catch (IllegalStateException _) {
            try {
                state.setFanspeed(SPEED_MAPPER.toEnum(new byte[]{dataBytes[0]}));
            } catch (IllegalStateException _) {
                try {
                    state.setMode(MODE_MAPPER.toEnum(new byte[]{dataBytes[0]}));
                } catch (IllegalStateException _) {
                    state.setPreset(PRESET_MAPPER.toEnum(new byte[]{dataBytes[0]}));
                }
            }
        }

        return state;
    }

    @Override
    protected Class<TemperatureControlState> getStateType() {
        return TemperatureControlState.class;
    }

    public enum ValidTemperatureControlMode {
        OFF,
        AUTO,
        HEAT,
        COOL,
        VENT,
        DRY;
    }

    public enum ValidTemperatureControlAction {
        TARGET(
                s -> TemperatureStateCalculator.bigDecimalToBytes(required("target_temperature", s.getTargetTemperature())),
                (c, s, b) -> s.setTargetTemperature(TemperatureStateCalculator.bytesToBigDecimal(c, new byte[]{b[1], b[2]}))
        ),
        // SET_TEMPSETDAY / SET_TEMPSETNIGHT: 1 byte = (°C + 40) x 2 (TDS15132, "Sensor values - SETS")
        DAY_PRESET(
                s -> presetToByte("day_preset_temperature", s.getDayPresetTemperature()),
                (c, s, b) -> s.setDayPresetTemperature(byteToPreset(b[1]))
        ),
        NIGHT_PRESET(
                s -> presetToByte("night_at_heating_preset_temperature", s.getNightAtHeatingPresetTemperature()),
                (c, s, b) -> s.setNightAtHeatingPresetTemperature(byteToPreset(b[1]))
        ),
        // SET_TEMPSETSTANDBY: the eco offset in steps of 0.5 °C (offset x 2). TDS15132 says "x 10", but a MICROS+
        // stored 1.5, 15.0, 0.5 and 4.0 °C when sent 3, 30, 1 and 8 (measured 2026-10-30, also on the wall panel).
        // Reports stay x 10 in an unsigned byte, so 25.5 °C is the most the central can report back.
        ECO_OFFSET(
                s -> UNSIGNED_BYTE.convert(inRange("eco_preset_offset", s.getEcoPresetOffset(), BigDecimal.ZERO, new BigDecimal("25.5")).multiply(BigDecimal.valueOf(2)).setScale(0, RoundingMode.HALF_UP)),
                (c, s, b) -> s.setEcoPresetOffset(new BigDecimal(Byte.toUnsignedInt(b[1])).divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP))
        );

        private static byte[] presetToByte(String name, BigDecimal celsius) {
            return UNSIGNED_BYTE.convert(inRange(name, celsius, BigDecimal.valueOf(-40), new BigDecimal("87.5")).add(BigDecimal.valueOf(40)).multiply(BigDecimal.valueOf(2)).setScale(0, RoundingMode.HALF_UP));
        }

        private static BigDecimal required(String name, BigDecimal value) {
            if (value == null) {
                throw new IllegalArgumentException("Missing '" + name + "'");
            }
            return value;
        }

        // A SET value is 1 byte, a value outside min .. max would silently wrap around
        private static BigDecimal inRange(String name, BigDecimal value, BigDecimal min, BigDecimal max) {
            if (required(name, value).compareTo(min) < 0 || value.compareTo(max) > 0) {
                throw new IllegalArgumentException("'" + name + "' must be between " + min + " and " + max + ", was " + value);
            }
            return value;
        }

        private static BigDecimal byteToPreset(byte value) {
            return new BigDecimal(Byte.toUnsignedInt(value)).divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_UP).subtract(BigDecimal.valueOf(40));
        }

        private final Function<TemperatureControlState, byte[]> getData;
        private final TriConsumer<ComponentSpec, TemperatureControlState, byte[]> applyData;

        ValidTemperatureControlAction() {
            this.getData = s -> new byte[]{};
            this.applyData = (c, s, d) -> {
            };
        }

        ValidTemperatureControlAction(
                Function<TemperatureControlState, byte[]> getData,
                TriConsumer<ComponentSpec, TemperatureControlState, byte[]> applyData
        ) {
            this.getData = getData;
            this.applyData = applyData;
        }

        public byte[] getData(TemperatureControlState state) {
            return this.getData.apply(state);
        }

        public void applyData(ComponentSpec componentSpec, TemperatureControlState state, byte[] data) {
            this.applyData.accept(componentSpec, state, data);
        }
    }

    public enum ValidTemperatureControlSpeed {
        LOW,
        MEDIUM,
        HIGH,
        AUTO;
    }

    public enum ValidTemperatureControlPreset {
        FROST,
        DAY,
        NIGHT,
        ECO,
        /**
         * Not a Teletask preset: reported (in HA) when no known preset is active, i.e. a manually set target.
         * Sending it re-applies the current target temperature.
         */
        MANUAL,
    }
}
