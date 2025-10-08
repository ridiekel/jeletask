package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.mapper.Mapper;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.TemperatureControlState;
import io.github.ridiekel.jeletask.utilities.Bytes;

public class TemperatureControlStateCalculator extends StateCalculatorSupport<TemperatureControlState> {

    private final double divide = 10;
    private final int subtract = 271;
    public static final OnOffToggleStateCalculator TEMPERATURE_CONTROL_STATE_CALCULATOR = new OnOffToggleStateCalculator();
    private static final Mapper<ValidTemperatureControlState> MAPPER = new Mapper<>(ValidTemperatureControlState.class, NumberConverter.UNSIGNED_BYTE)
            .add(ValidTemperatureControlState.MANUAL, 0)
            .add(ValidTemperatureControlState.UP, 21)
            .add(ValidTemperatureControlState.DOWN, 22)
            .add(ValidTemperatureControlState.TARGET, 87)
            .add(ValidTemperatureControlState.FROST, 24)
            .add(ValidTemperatureControlState.DAY, 26)
            .add(ValidTemperatureControlState.NIGHT, 25)
            .add(ValidTemperatureControlState.ECO, 93)
            .add(ValidTemperatureControlState.SETDAY, 29)         // Not implemented
            .add(ValidTemperatureControlState.SETECO, 88)         // Not implemented
            .add(ValidTemperatureControlState.SETNIGHTHEAT, 27)   // Not implemented
            .add(ValidTemperatureControlState.SETNIGHTCOOL, 56)   // Not implemented
            .add(ValidTemperatureControlState.SPEED, 31)
            .add(ValidTemperatureControlState.SPLOW, 97)
            .add(ValidTemperatureControlState.SPMED, 98)
            .add(ValidTemperatureControlState.SPHIGH, 99)
            .add(ValidTemperatureControlState.SPAUTO, 89)
            .add(ValidTemperatureControlState.MODE, 30)
            .add(ValidTemperatureControlState.AUTO, 94)
            .add(ValidTemperatureControlState.HEAT, 95)
            .add(ValidTemperatureControlState.COOL, 96)
            .add(ValidTemperatureControlState.VENT, 105)
            .add(ValidTemperatureControlState.STOP, 106)
            .add(ValidTemperatureControlState.HEATP, 107)         // What is Heat+ ?
            .add(ValidTemperatureControlState.DRY, 108)
            .add(ValidTemperatureControlState.ON, 255)
            .add(ValidTemperatureControlState.OFF, 0)
            .add(ValidTemperatureControlState.ONOFF, 104);

    public enum ValidTemperatureControlState {
        MANUAL, UP, DOWN, TARGET, FROST, DAY, NIGHT, ECO, SETDAY, SETECO, SETNIGHTHEAT, SETNIGHTCOOL, SPEED, SPLOW, SPMED, SPHIGH, SPAUTO, MODE, AUTO, HEAT, COOL, VENT, STOP, HEATP, DRY, ON, OFF, ONOFF
    }

    @Override
    protected Class<TemperatureControlState> getStateType() {
        return TemperatureControlState.class;
    }

    @Override
    public TemperatureControlState fromEvent(ComponentSpec component, byte[] dataBytes) {
        if (dataBytes.length < 17)
            return null;

        TemperatureControlState state = new TemperatureControlState(TEMPERATURE_CONTROL_STATE_CALCULATOR.fromEvent(component, new byte[]{dataBytes[12]}).getState());
        state.setCurrentTemperature((NumberConverter.UNSIGNED_SHORT.convert(new byte[]{dataBytes[0], dataBytes[1]}).longValue() / this.divide) - this.subtract);
        state.setTargetTemperature((NumberConverter.UNSIGNED_SHORT.convert(new byte[]{dataBytes[2], dataBytes[3]}).longValue() / this.divide) - this.subtract);
        state.setDayPresetTemperature((NumberConverter.UNSIGNED_SHORT.convert(new byte[]{dataBytes[4], dataBytes[5]}).longValue() / this.divide) - this.subtract);
        state.setNightAtHeatingPresetTemperature((NumberConverter.UNSIGNED_SHORT.convert(new byte[]{dataBytes[6], dataBytes[7]}).longValue() / this.divide) - this.subtract);
        state.setEcoPresetOffset((float) dataBytes[8] / this.divide);
        state.setPreset(MAPPER.toString(new byte[]{dataBytes[9]}));

        if (ValidTemperatureControlState.OFF.name().equals(state.getState().name())) {
            state.setMode(ValidTemperatureControlState.OFF.name());
        } else {
            state.setMode(MAPPER.toString(new byte[]{dataBytes[10]}));
        }

        state.setFanspeed(MAPPER.toString(new byte[]{dataBytes[11]}));
        state.setWindowOpen((int) dataBytes[13]);       // 0 = open, 255 = closed
        state.setOutputState((int) dataBytes[14]);      // Untested, no idea about the values
        state.setSwingDirection((int) dataBytes[15]);   // Untested, no idea about the values
        state.setNightAtCoolingPresetTemperature((NumberConverter.UNSIGNED_SHORT.convert(new byte[]{dataBytes[16], dataBytes[17]}).longValue() / this.divide) - this.subtract);

        return state;
    }

    @Override
    public byte[] toCommand(TemperatureControlState state) {
        byte[] setting = null;
        byte[] data = Bytes.EMPTY;

        /*
            TODO:   Add support to changing preset temperatures (SETDAY, SETECO, SETNIGHTHEAT, SETNIGHTCOOL).
                    Needs to be implemented the same way we already did for "TARGET" (see below).
         */

        if (state.getTargetTemperature() != null) {
            setting = MAPPER.toBytes(ValidTemperatureControlState.TARGET);
            try {
                int temperature = state.getTargetTemperature().intValue();
                data = NumberConverter.UNSIGNED_SHORT.convert((temperature + this.subtract) * this.divide);
            } catch (NumberFormatException e) {
                //Not a number, so no data is needed
            }
        }

        if (setting == null) {
            setting = MAPPER.toBytes(ValidTemperatureControlState.valueOf(state.getMode()));
        }

        return Bytes.concat(setting, data);
    }

    @Override
    public boolean isValidWriteState(TemperatureControlState state) {
        return state.getTargetTemperature() != null && super.isValidWriteState(state);
    }
}
