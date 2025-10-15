package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.DimmerState;

/**
 * For dimmers the setting parameter needs to be 0-100 indicating the level to which you want to set the dimmer.
 *
 * <pre>
 * +---------------------+---------+------------------------------------------------------------------+
 * | Constant            | Value   | Description                                                      |
 * +---------------------+---------+------------------------------------------------------------------+
 * | SET_DIM             | 0 - 100 | The level to which you want to set the dimmer                    |
 * +---------------------+---------+------------------------------------------------------------------+
 * </pre>
 */
public class DimmerStateCalculator extends StateCalculatorSupport<DimmerState> {
    public DimmerStateCalculator() {
    }

    @Override
    public DimmerState fromEvent(ComponentSpec component, byte[] dataBytes) {
        Number number = NumberConverter.UNSIGNED_BYTE.convert(new byte[]{dataBytes[0]});

        DimmerState state = new DimmerState((number.intValue() > 0) ? ValidDimmerState.ON : ValidDimmerState.OFF);
        state.setBrightness(number.intValue());

        return state;
    }

    @Override
    public byte[] toCommand(DimmerState state) {
        byte[] bytes = null;
        if (state != null) {
            if (state.getBrightness() != null) {
                // Brightness attribute always has priority
                bytes = NumberConverter.UNSIGNED_BYTE.convert(state.getBrightness());
            }
        }

        return bytes;
    }

    @Override
    protected Class<DimmerState> getStateType() {
        return DimmerState.class;
    }

    public enum ValidDimmerState {
        ON, OFF, PREVIOUS_STATE
    }

    @Override
    public boolean isValidWriteState(DimmerState state) {
        boolean isValid = false;

        if (super.isValidWriteState(state)) {
            if (state.getBrightness() != null) {
                int brightness = state.getBrightness().intValue();
                isValid = (brightness >= 0 && brightness <= 100);
            }
        }

        return isValid;
    }
}

