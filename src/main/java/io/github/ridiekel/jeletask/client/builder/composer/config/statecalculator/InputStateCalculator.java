
package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.mapper.Mapper;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.InputState;

/**
 * For FNC_TPKEY number is the input number.
 * Setting can be SET_PULSE, SET_CLOSED or SET_OPENED.
 *
 * <pre>
 * +--------------+-------+--------------------------------------+
 * | Constant     | Value | Description                          |
 * +--------------+-------+--------------------------------------+
 * | SET_PULSE    | 1     | For a "Short" press                  |
 * | SET_CLOSED   | 2     | For a long press                     |
 * | SET_OPENED   | 3     | For the end of a long press          |
 * +--------------+-------+--------------------------------------+
 * </pre>
 */
public class InputStateCalculator extends StateCalculatorSupport<InputState> {
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
