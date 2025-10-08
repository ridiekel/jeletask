package io.github.ridiekel.jeletask.client.spec.state.impl;

import io.github.ridiekel.jeletask.client.spec.state.State;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class PulseCounterState extends State<String> {
    private Number current;
    private Number total;

    public PulseCounterState() {
    }

    public PulseCounterState(String state) {
        super(state);
    }

    public Number getCurrent() {
        return current;
    }

    public void setCurrent(Number current) {
        this.current = current;
    }

    public Number getTotal() {
        return total;
    }

    public void setTotal(Number total) {
        this.total = total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        PulseCounterState that = (PulseCounterState) o;

        return new EqualsBuilder().appendSuper(super.equals(o)).append(current, that.current).append(total, that.total).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(current).append(total).toHashCode();
    }
}
