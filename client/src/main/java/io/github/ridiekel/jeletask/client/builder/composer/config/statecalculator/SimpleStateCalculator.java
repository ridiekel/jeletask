package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.utilities.Bytes;

public class SimpleStateCalculator implements StateCalculator {
    private final NumberConverter numberConverter;

    public SimpleStateCalculator(NumberConverter numberConverter) {
        this.numberConverter = numberConverter;
    }

    @Override
    public ComponentState toComponentState(ComponentSpec component, byte[] dataBytes) {
        Number number = this.numberConverter.convert(dataBytes);
        return new ComponentState(number.intValue() == -1 ? 255 : number);
    }

    @Override
    public byte[] toBytes(ComponentState value) {
        return value == null ? null : this.numberConverter.convert(value.getState());
    }

    @Override
    public NumberConverter getNumberConverter() {
        return this.numberConverter;
    }

    @Override
    public boolean isValidState(ComponentState state) {
        return true;
    }

    @Override
    public ComponentState getDefaultState(ComponentSpec component) {
        return new ComponentState("0");
    }
}
