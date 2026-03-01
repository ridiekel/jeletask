package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.DimmerState;
import org.jspecify.annotations.NonNull;

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
    public byte[] toCommand(ComponentSpec component, DimmerState state) {
        Integer brightness = determineTargetBrightness(component, state);

        return NumberConverter.UNSIGNED_BYTE.convert(brightness);
    }

    public static @NonNull Integer determineTargetBrightness(ComponentSpec component, DimmerState state) {
        Integer brightness = null;

        if (state.getState() == ValidDimmerState.OFF) {
            brightness = 0;
        } else {
            brightness = state.getBrightness();
        }

        if (brightness == null) {
            brightness = 103;
        }
        return brightness;
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
        boolean isValid = super.isValidWriteState(state);

        if (isValid) {
            if (state.getBrightness() != null) {
                int brightness = state.getBrightness().intValue();
                isValid = (brightness >= 0 && brightness <= 100);
            }
        }

        return isValid;
    }
}

