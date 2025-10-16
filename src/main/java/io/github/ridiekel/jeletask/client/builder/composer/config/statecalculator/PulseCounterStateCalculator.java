package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.PulseCounterState;

/**
 * For pulse counter sensor there are two recalculations possible:
 * <p>
 * - The real time value, with
 * o PU = number of pulses per unit
 * o rtU = number of realtime Units per unit
 * o sec = number of seconds in the timebase
 * <p>
 * can be calculated as
 * Value = (short * rtU * sec ) / (3600 * PU)
 * <p>
 * The total value (note this is a four byte value (int32)):
 * Value = int32 / number of pulses per Unit
 */
public class PulseCounterStateCalculator extends StateCalculatorSupport<PulseCounterState> {
    @Override
    protected Class<PulseCounterState> getStateType() {
        return PulseCounterState.class;
    }

    @Override
    public PulseCounterState fromEvent(ComponentSpec component, byte[] dataBytes) {

        PulseCounterState state = new PulseCounterState("PULSECOUNTER");

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

    @Override
    public byte[] toCommand(ComponentSpec component, PulseCounterState state) {
        throw new IllegalArgumentException("Pulse counter is read only. Strange that we get in the serialize method");
    }
}


