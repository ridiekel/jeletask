package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.mapper.Mapper;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;

/**
 * For most functions the "Data" parameters are not used.
 * The setting parameter can be SET_ON, SET_OFF or SET_TOGGLE.
 *
 * <pre>
 * +---------------------+-------+------------------------------------------------------------------+
 * | Constant            | Value | Description                                                      |
 * +---------------------+-------+------------------------------------------------------------------+
 * | SET_ON              | 255   | Turn ON                                                          |
 * | SET_TOGGLE          | 103   | Toggle ON/OFF                                                    |
 * | SET_OFF             | 0     | Turn OFF                                                         |
 * +---------------------+-------+------------------------------------------------------------------+
 * </pre>
 */
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
    public byte[] toCommand(ComponentSpec component, OnOffState state) {
        return MAPPER.toBytes(state.getState());
    }

    @Override
    protected Class<OnOffState> getStateType() {
        return OnOffState.class;
    }
}
