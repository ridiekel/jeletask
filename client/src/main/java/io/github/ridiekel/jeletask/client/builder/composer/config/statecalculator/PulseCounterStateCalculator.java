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

        /*
        Bytes example: 06 3A 00 00 00 00 00 00 00 1A 00 00 FF 00 00 00 00 00 00 00 F9
        Explanation from Teletask:
            0+1         = real time value in watt   = 0x063A
            2+3         = current set point in watt = 0x0000
            4+5         = preset 1 (day)            = 0x0000
            6+7         = preset 2 (night)          = 0x0000
            8           = /
            9           = current preset selection  = 0x1A = 26=day
            10          = /
            11          = /
            12          = on/off state              = 0xFF   = 255  = on (0 = off)
            13          = /
            14          = /
            15          = /
            16+17+18+19 = consumption in kWh = 0x00000000 = 0
        */        
        
        state.setCurrent(NumberConverter.UNSIGNED_SHORT.convert(new byte[]{dataBytes[0],dataBytes[1]}).shortValue());
        state.setTotal(NumberConverter.UNSIGNED_INT.convert(new byte[]{dataBytes[16],dataBytes[17],dataBytes[18],dataBytes[19]}).floatValue() / component.getPulses_per_unit());

        return state;

    }
}


