
package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.mapper.Mapper;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.InputState;

public class InputStateCalculator extends StateCalculatorSupport<InputState> {
    private static final Mapper<ValidInputState> MAPPER = new Mapper<>(ValidInputState.class, NumberConverter.UNSIGNED_BYTE)
            .add(ValidInputState.CLOSED, 2)
            .add(ValidInputState.OPEN, 3);

    @Override
    public InputState fromEvent(ComponentSpec component, byte[] dataBytes) {
        return new InputState(MAPPER.toEnum(dataBytes));
    }

    public enum ValidInputState {
        OPEN, CLOSED, LONG_PRESS, SHORT_PRESS, NOT_PRESSED
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
