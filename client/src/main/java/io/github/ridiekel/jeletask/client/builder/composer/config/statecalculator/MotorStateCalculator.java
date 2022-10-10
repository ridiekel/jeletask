package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.utilities.Bytes;

import java.util.List;

public class MotorStateCalculator extends MappingStateCalculator {
    private static final List<StateMapping> STATE_MAPPINGS = List.of(
            new StateMapping("UP", 1),
            new StateMapping("DOWN", 2),
            new StateMapping("STOP", 3),
            new StateMapping("START_STOP", 6),
            new StateMapping("UP_STOP", 7),
            new StateMapping("DOWN_STOP", 8),
            new StateMapping("UP_DOWN", 55),
            new StateMapping("MOTOR_GO_TO_POSITION", 11),
            new StateMapping("MOTOR_SUN_PROTECTION", 15)
    );

    public static final OnOffToggleStateCalculator MOTOR_STATE_CALCULATOR = new OnOffToggleStateCalculator(NumberConverter.UNSIGNED_BYTE, 255, 0);

    private static final MappingStateCalculator PROTECTION_STATE_CALCULATOR = new MappingStateCalculator(NumberConverter.UNSIGNED_BYTE,
            new StateMapping("NOT_DEFINED", 0),
            new StateMapping("ON_MOTOR_CONTROLLED_BY_PROTECTION", 1),
            new StateMapping("ON_MOTOR_NOT_CONTROLLED_BY_PROTECTION", 2),
            new StateMapping("ON_OVERRULED_BY_USER", 3),
            new StateMapping("SWITCHED_OFF", 4)
    );

    public MotorStateCalculator(NumberConverter numberConverter) {
        super(numberConverter, STATE_MAPPINGS.toArray(StateMapping[]::new));
    }

    @Override
    public ComponentState toComponentState(ComponentSpec component, byte[] dataBytes) {
        ComponentState state = new ComponentState(MOTOR_STATE_CALCULATOR.toComponentState(component, new byte[]{dataBytes[1]}).getState());
        state.setLastDirection(super.toComponentState(component, dataBytes).getState());
        state.setProtection(PROTECTION_STATE_CALCULATOR.toComponentState(component, new byte[]{dataBytes[2]}).getState());
        state.setPosition(NumberConverter.UNSIGNED_BYTE.convert(new byte[]{dataBytes[3]}));
        if (dataBytes.length > 4) {
            state.setCurrentPosition(NumberConverter.UNSIGNED_BYTE.convert(new byte[]{dataBytes[4]}));
        } else {
            state.setCurrentPosition(NumberConverter.UNSIGNED_BYTE.convert(new byte[]{dataBytes[3]}));
        }
        if (dataBytes.length > 6) {
            state.setSecondsToFinish( NumberConverter.UNSIGNED_SHORT.convert(new byte[]{dataBytes[5], dataBytes[6]}).floatValue() / 100);
        }
        if (dataBytes.length > 7) {
            state.setCorrectionAtZeroPercentInSeconds(NumberConverter.UNSIGNED_BYTE.convert(new byte[]{dataBytes[7]}));
        }
        if (dataBytes.length > 8) {
            state.setCorrectionAtHundredPercentInSeconds(NumberConverter.UNSIGNED_BYTE.convert(new byte[]{dataBytes[8]}));
        }
        return state;
    }


    @Override
    public byte[] toBytes(ComponentState state) {
        byte[] setting = null;

        byte[] data = Bytes.EMPTY;

        if (state.getPosition() != null) {
            setting = super.toBytes(new ComponentState("MOTOR_GO_TO_POSITION"));

            try {
                int position = (int) state.getPosition();
                position = Math.min(position, 100);
                position = Math.max(position, 0);
                data = NumberConverter.UNSIGNED_BYTE.convert(position);
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
        return state.getPosition() != null || super.isValidState(state);
    }
}
