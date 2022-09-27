package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.utilities.Bytes;

import java.util.List;

public class AurusStateCalculator extends MappingStateCalculator {

    public static final OnOffToggleStateCalculator AURUS_STATE_CALCULATOR = new OnOffToggleStateCalculator(NumberConverter.UNSIGNED_BYTE, 255, 0);

    private static final List<StateMapping> STATE_MAPPINGS = List.of(
            new StateMapping("UP", 21),
            new StateMapping("DOWN", 22),
            new StateMapping("FROST", 24),
            new StateMapping("DAY", 26),
            new StateMapping("NIGHT",25 ),
            new StateMapping("STANDBY", 93),
//            new StateMapping("SETDAY", 29),
//            new StateMapping("SETSTANDBY", 88),
//            new StateMapping("SETNIGHT", 27),
//            new StateMapping("SETNIGHTCOOL", 56),
//            new StateMapping("TEMPMANUAL_TARGET", 87), // Not verified yet
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
            new StateMapping("HEATP", 107),
            new StateMapping("DRY", 108), // Not verified yet
            new StateMapping("ONOFF", 104)
    );
    public AurusStateCalculator(NumberConverter numberConverter) {
        super(numberConverter, STATE_MAPPINGS.toArray(StateMapping[]::new));
    }

    @Override
    public ComponentState toComponentState(ComponentSpec component, byte[] dataBytes) {
        if (dataBytes.length < 17)
            return null;

        ComponentState state = new ComponentState(AURUS_STATE_CALCULATOR.toComponentState(component, new byte[]{dataBytes[12]}).getState());
        TemperatureStateCalculator tempcalculator = new TemperatureStateCalculator(NumberConverter.UNSIGNED_SHORT, 10, 273);

        state.setCurrentTemperature(Float.valueOf(tempcalculator.toComponentState(component, new byte[]{dataBytes[0], dataBytes[1]}).getState()));
        state.setTargetTemperature(Float.valueOf(tempcalculator.toComponentState(component, new byte[]{dataBytes[2], dataBytes[3]}).getState()));

        // TODO: reverse engineer these additional bytes? Or ask Teletask for more info?
        // We need to complete toComponentState() here...
        // Byte 0+1 = current temperature
        // Byte 2+3 = target temperature
        // Byte 4-11 = Unknown. Need to reverse engineer...
        // Byte 12 = ON/OFF state (255/0)
        // Byte 13-17 = Unknown. Need to reverse engineer...

        return state;
    }

    @Override
    public byte[] toBytes(ComponentState state) {
        byte[] setting = null;
        byte[] data = Bytes.EMPTY;

        // TODO: implement commands with parameters: SETDAY,SETSTANDY,SETNIGHT,SETNIGHTCOOL,TEMPMANUAL_TARGET

        if (setting == null) {
            setting = super.toBytes(state);
        }

        return Bytes.concat(setting, data);
    }

    @Override
    public boolean isValidState(ComponentState state) {
        return state.getState() != null || super.isValidState(state);
    }


}
