package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

import java.util.List;

public class DimmerStateCalculator extends MappingStateCalculator {

    private static final List<StateMapping> STATE_MAPPINGS = List.of(
            new StateMapping("ON", 100),
            new StateMapping("OFF", 0),
            new StateMapping("PREVIOUS_STATE", 103)
    );

    public DimmerStateCalculator(NumberConverter numberConverter) {
        super(numberConverter, STATE_MAPPINGS.toArray(StateMapping[]::new));
    }

    @Override
    public ComponentState getDefaultState(ComponentSpec component) {
        return new ComponentState("50");
    }

    @Override
    public ComponentState toComponentState(ComponentSpec component, byte[] dataBytes) {
        Number number = NumberConverter.UNSIGNED_BYTE.convert(new byte[]{dataBytes[0]});

        ComponentState state = new ComponentState((number.intValue() > 0) ? "ON" : "OFF");
        state.setBrightness(number);

        return state;
    }

    @Override
    public byte[] toBytes(ComponentState state) {
        byte[] bytes = null;
        if (state != null) {
            if (state.getBrightness() != null) {
                // Brightness attribute always has priority
                bytes = NumberConverter.UNSIGNED_BYTE.convert(state.getBrightness());
            } else if (isStateNumber(state)) {
                bytes = NumberConverter.UNSIGNED_BYTE.convert(state.getState());
            } else{
                bytes = super.toBytes(new ComponentState(state.getState()));
            }
        }

        return bytes;
    }

    private static boolean isStateNumber(ComponentState state) {
        return state.getState().chars().allMatch(Character::isDigit);
    }

    @Override
    public boolean isValidState(ComponentState state) {
        boolean valid_brightness = false;

        if (state.getBrightness() != null) {
            byte brightness = state.getBrightness().byteValue();
            valid_brightness = (brightness >= 0 && brightness <= 100);
        } if (state.getState() != null && isStateNumber(state)) {
            int brightness = Integer.parseInt(state.getState());
            valid_brightness = (brightness >= 0 && brightness <= 100);
        }

        return valid_brightness || super.isValidState(state);
    }
}

