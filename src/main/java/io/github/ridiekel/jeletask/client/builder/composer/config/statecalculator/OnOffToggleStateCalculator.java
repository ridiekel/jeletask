package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.mapper.Mapper;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;

public class OnOffToggleStateCalculator extends StateCalculatorSupport<OnOffState> {
    private static final Mapper<ValidOnOffToggle> MAPPER = new Mapper<>(ValidOnOffToggle.class, NumberConverter.UNSIGNED_BYTE)
            .add(ValidOnOffToggle.ON, 255)
            .add(ValidOnOffToggle.OFF, 0)
            .add(ValidOnOffToggle.TOGGLE, 103);

    @Override
    public OnOffState fromEvent(ComponentSpec component, byte[] dataBytes) {
        return new OnOffState(MAPPER.toEnum(dataBytes));
    }

    public enum ValidOnOffToggle {
        ON, OFF, TOGGLE
    }

    @Override
    public byte[] toCommand(OnOffState state) {
        return MAPPER.toBytes(state.getState());
    }

    @Override
    protected Class<OnOffState> getStateType() {
        return OnOffState.class;
    }
}
