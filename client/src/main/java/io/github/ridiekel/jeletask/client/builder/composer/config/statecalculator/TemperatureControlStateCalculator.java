package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.utilities.Bytes;

import java.util.List;

public class TemperatureControlStateCalculator extends MappingStateCalculator {

    private final double divide;
    private final int subtract;
    public static final OnOffToggleStateCalculator TEMPERATURE_CONTROL_STATE_CALCULATOR = new OnOffToggleStateCalculator(NumberConverter.UNSIGNED_BYTE, 255, 0);

    private static final List<StateMapping> STATE_MAPPINGS = List.of(
            new StateMapping("MANUAL", 0),
            new StateMapping("UP", 21),
            new StateMapping("DOWN", 22),
            new StateMapping("TARGET", 87),
            new StateMapping("FROST", 24),
            new StateMapping("DAY", 26),
            new StateMapping("NIGHT",25 ),
            new StateMapping("STANDBY", 93),        // On the Aurus this mode is called "ECO" ?
            new StateMapping("SETDAY", 29),         // Not implemented
            new StateMapping("SETSTANDBY", 88),     // Not implemented
            new StateMapping("SETNIGHT", 27),       // Not implemented
            new StateMapping("SETNIGHTCOOL", 56),   // Not implemented
            new StateMapping("SPEED", 31),
            new StateMapping("SPLOW", 97),
            new StateMapping("SPMED", 98),
            new StateMapping("SPHIGH", 99),
            new StateMapping("SPAUTO", 89),
            new StateMapping("MODE", 30),
            new StateMapping("AUTO", 94),
            new StateMapping("HEAT", 95),
            new StateMapping("COOL", 96),
            new StateMapping("VENT", 105),
            new StateMapping("STOP", 106),
            new StateMapping("HEATP", 107),         // What is Heat+ ?
            new StateMapping("DRY", 108),
            new StateMapping("ON", 255),
            new StateMapping("OFF", 0),
            new StateMapping("ONOFF", 104)
    );

    public TemperatureControlStateCalculator(NumberConverter numberConverter, int divide, int subtract) {
        super(numberConverter, STATE_MAPPINGS.toArray(StateMapping[]::new));
        this.divide = divide;
        this.subtract = subtract;
    }

    @Override
    public ComponentState toComponentState(ComponentSpec component, byte[] dataBytes) {
        if (dataBytes.length < 17)
            return null;

        ComponentState state = new ComponentState(TEMPERATURE_CONTROL_STATE_CALCULATOR.toComponentState(component, new byte[]{dataBytes[12]}).getState());
        state.setCurrentTemperature(Float.valueOf(new ComponentState((NumberConverter.UNSIGNED_SHORT.convert(new byte[]{dataBytes[0], dataBytes[1]}).longValue() / this.divide) - this.subtract).getState()));
        state.setTargetTemperature(Float.valueOf(new ComponentState((NumberConverter.UNSIGNED_SHORT.convert(new byte[]{dataBytes[2], dataBytes[3]}).longValue() / this.divide) - this.subtract).getState()));
        state.setDayPresetTemperature(Float.valueOf(new ComponentState((NumberConverter.UNSIGNED_SHORT.convert(new byte[]{dataBytes[4], dataBytes[5]}).longValue() / this.divide) - this.subtract).getState()));
        state.setNightAtHeatingPresetTemperature(Float.valueOf(new ComponentState((NumberConverter.UNSIGNED_SHORT.convert(new byte[]{dataBytes[6], dataBytes[7]}).longValue() / this.divide) - this.subtract).getState()));
        state.setEcoPreset( (float) dataBytes[8] / this.divide );
        state.setPreset(super.toComponentState(component, new byte[]{dataBytes[9]}).getState());

        if ("OFF".equals(state.getState()))
            state.setMode("OFF");
        else
            state.setMode(super.toComponentState(component, new byte[]{dataBytes[10]}).getState());

        state.setFanspeed(super.toComponentState(component, new byte[]{dataBytes[11]}).getState());

        // Byte 13 = Unknown (0x00 ?)
        // Byte 14 = Unknown (0x80 / 0x90 ?)
        // Byte 15 = Unknown (0x00 ?)

        state.setNightAtCoolingPresetTemperature(Float.valueOf(new ComponentState((NumberConverter.UNSIGNED_SHORT.convert(new byte[]{dataBytes[16], dataBytes[17]}).longValue() / this.divide) - this.subtract).getState()));

        return state;
    }

    @Override
    public byte[] toBytes(ComponentState state) {
        byte[] setting = null;
        byte[] data = Bytes.EMPTY;

        if (state.getTargetTemperature() != null) {
            setting = super.toBytes(new ComponentState("TARGET"));
            try {
                int temperature = (int) state.getTargetTemperature().intValue();
                data = NumberConverter.UNSIGNED_SHORT.convert((temperature+this.subtract)*this.divide);
            } catch (NumberFormatException e) {
                //Not a number, so no data is needed
            }
        }

        if (setting == null) {
            setting = super.toBytes(state);
        }

        return Bytes.concat(setting, data);
    }

    @Override
    public boolean isValidState(ComponentState state) {
        return state.getTargetTemperature() != null || super.isValidState(state);
    }


}
