package io.github.ridiekel.jeletask.client.builder.message.messages;

import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Function;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public abstract class FunctionBasedMessageSupport extends MessageSupport {
    private final Function function;

    protected FunctionBasedMessageSupport(CentralUnit centralUnit, Function function) {
        super(centralUnit);
        this.function = function;
    }

    public Function getFunction() {
        return this.function;
    }

    @Override
    protected String getId() {
        return this.function.name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        FunctionBasedMessageSupport that = (FunctionBasedMessageSupport) o;

        return new EqualsBuilder().appendSuper(super.equals(o)).append(function, that.function).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(function).toHashCode();
    }
}
