package io.github.ridiekel.jeletask.client.spec.state.impl;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.DimmerStateCalculator;
import io.github.ridiekel.jeletask.client.spec.state.State;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class DimmerState extends State<DimmerStateCalculator.ValidDimmerState> {
    private Integer brightness;

    public DimmerState() {
    }

    public DimmerState(DimmerStateCalculator.ValidDimmerState state) {
        super(state);
    }

    public DimmerState(Integer brightness) {
        this(brightness > 0 ? DimmerStateCalculator.ValidDimmerState.ON : DimmerStateCalculator.ValidDimmerState.OFF);
        this.setBrightness(brightness);
    }

    public DimmerState(DimmerStateCalculator.ValidDimmerState state, int i) {
        super(state);
        this.setBrightness(i);
    }

    public Integer getBrightness() {
        return brightness;
    }

    public void setBrightness(Integer brightness) {
        this.brightness = brightness;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        DimmerState that = (DimmerState) o;

        return new EqualsBuilder().appendSuper(super.equals(o)).append(brightness, that.brightness).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(brightness).toHashCode();
    }
}
