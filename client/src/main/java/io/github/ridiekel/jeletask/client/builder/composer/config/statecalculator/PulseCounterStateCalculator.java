package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

public class PulseCounterStateCalculator extends SimpleStateCalculator {

    public PulseCounterStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }

    @Override
    public ComponentState toComponentState(ComponentSpec component, byte[] dataBytes) {
        /*

    private double GetDisplayValue(final int val, final boolean realtime, final byte decimals) {
        if (val == 0) {
            return 0.0;
        }
        double waarde;
        if (realtime) {
            if (!this.mShowRealTime) {
                waarde = val * (1000.0 / this.mPulsePerUnit);
            }
            else {
                waarde = val * (this.mRtUnitsPerUnit * (double)this.SecondsPerTimeBase(this.mTimeBase)) / (3600.0 * this.mPulsePerUnit);
            }
        }
        else {
            waarde = val / (double)this.mPulsePerUnit;
        }
        final int temp = (int)(waarde * Math.pow(10.0, decimals));
        return temp / Math.pow(10.0, decimals);
    }


         */
        return new ComponentState("TODO");
    }
}


