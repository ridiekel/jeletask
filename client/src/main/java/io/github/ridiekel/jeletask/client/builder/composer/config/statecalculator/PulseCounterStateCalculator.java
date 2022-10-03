package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PulseCounterStateCalculator extends SimpleStateCalculator {

    public PulseCounterStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }

    @Override
    public ComponentState toComponentState(ComponentSpec component, byte[] dataBytes) {

        ComponentState state = new ComponentState("PULSECOUNTER");

        state.setCurrent(NumberConverter.UNSIGNED_SHORT.convert(new byte[]{dataBytes[0],dataBytes[1]}).shortValue());
        state.setTotal(NumberConverter.UNSIGNED_INT.convert(new byte[]{dataBytes[16],dataBytes[17],dataBytes[18],dataBytes[19]}).floatValue() / component.getPulses_per_unit());

        return state;

    }
}


