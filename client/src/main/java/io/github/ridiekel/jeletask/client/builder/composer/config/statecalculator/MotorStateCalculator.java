package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.utilities.Bytes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class MotorStateCalculator extends MappingStateCalculator {
    private static final List<StateMapping> STATE_MAPPINGS = new ArrayList<>();
    static {
        STATE_MAPPINGS.add(new StateMapping("UP", 1));
        STATE_MAPPINGS.add(new StateMapping("DOWN", 2));
        STATE_MAPPINGS.add(new StateMapping("STOP", 3));
        STATE_MAPPINGS.add(new StateMapping("START_STOP", 6));
        STATE_MAPPINGS.add(new StateMapping("UP_STOP", 7));
        STATE_MAPPINGS.add(new StateMapping("DOWN_STOP", 8));
        STATE_MAPPINGS.add(new StateMapping("UP_DOWN", 55));
        IntStream.range(0, 101).forEach(i -> STATE_MAPPINGS.add(new StateMapping(String.valueOf(i), 11)));
    }

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
    public ComponentState convertGet(byte[] dataBytes) {
        ComponentState state = new ComponentState(MOTOR_STATE_CALCULATOR.convertGet(new byte[]{dataBytes[1]}).getState());
        state.setDirection(super.convertGet(dataBytes).getState());
        state.setProtection(PROTECTION_STATE_CALCULATOR.convertGet(new byte[]{dataBytes[2]}).getState());
        state.setPosition(NumberConverter.UNSIGNED_BYTE.convert(new byte[]{dataBytes[3]}));
        return state;
    }

    @Override
    public byte[] convertSetState(ComponentState state) {
        byte[] bytes = Bytes.EMPTY;
        try {
            int data = Integer.parseInt(state.getState());
            data = Math.min(data, 100);
            data = Math.max(data, 0);
            bytes = NumberConverter.UNSIGNED_BYTE.convert(data);
        } catch (NumberFormatException e) {
            //Not a number, so no data is needed
        }
        return bytes;
    }
}
