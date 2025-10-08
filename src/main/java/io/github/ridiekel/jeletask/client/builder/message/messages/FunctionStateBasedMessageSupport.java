package io.github.ridiekel.jeletask.client.builder.message.messages;

import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public abstract class FunctionStateBasedMessageSupport<S extends State<?>> extends FunctionBasedMessageSupport {
    private final S state;

    protected FunctionStateBasedMessageSupport(CentralUnit centralUnit, Function function, S state) {
        super(centralUnit, function);
        this.state = state;
        if (state != null) {
        }
    }

    public S getState() {
        return this.state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        FunctionStateBasedMessageSupport<?> that = (FunctionStateBasedMessageSupport<?>) o;

        return new EqualsBuilder().appendSuper(super.equals(o)).append(state, that.state).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(state).toHashCode();
    }
}
