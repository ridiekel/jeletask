
package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.mapper.Mapper;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.InputState;

public class InputStateCalculator extends StateCalculatorSupport<InputState> {
    /*
Close short press: 1
Close long press: 2

Close edge triggered: 2
Open edge triggered: 3

Inverted and button control returns 3 then 2 then 9??
     */
    private static final Mapper<ValidInputState> MAPPER = new Mapper<>(ValidInputState.class, NumberConverter.UNSIGNED_BYTE)
            .add(ValidInputState.UNKNOWN_0, 0)
            .add(ValidInputState.SHORT_PRESS, 1)
            .add(ValidInputState.CLOSED, 2)
            .add(ValidInputState.OPEN, 3)
            .add(ValidInputState.UNKNOWN_9, 9);

    @Override
    public InputState fromEvent(ComponentSpec component, byte[] dataBytes) {
        return new InputState(MAPPER.toEnum(dataBytes));
    }

    public enum ValidInputState {
        UNKNOWN_0, OPEN, CLOSED, LONG_PRESS, SHORT_PRESS, NOT_PRESSED, UNKNOWN_9
    }

    @Override
    public byte[] toCommand(InputState state) {
        return MAPPER.toBytes(state.getState());
    }

    @Override
    protected Class<InputState> getStateType() {
        return InputState.class;
    }
}
