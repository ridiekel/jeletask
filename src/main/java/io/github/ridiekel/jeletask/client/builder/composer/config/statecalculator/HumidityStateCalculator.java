package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.HumidityState;

import java.math.BigDecimal;

/**
 * Byte = % humidity
 */
public class HumidityStateCalculator extends StateCalculatorSupport<HumidityState> {
    public static final NumberConverter NUMBER_CONVERTER = NumberConverter.UNSIGNED_SHORT;

    @Override
    public HumidityState fromEvent(ComponentSpec component, byte[] dataBytes) {
        long number = NUMBER_CONVERTER.convert(dataBytes).longValue();
        return new HumidityState(BigDecimal.valueOf(number == -1 ? 255 : number));
    }

    @Override
    public byte[] toCommand(ComponentSpec component, HumidityState state) {
        return NUMBER_CONVERTER.convert(state.getState());
    }

    @Override
    public boolean isValidWriteState(HumidityState state) {
        long value = state.getState().longValue();
        return value >= 0 && value <= 100;
    }

    @Override
    protected Class<HumidityState> getStateType() {
        return HumidityState.class;
    }
}
