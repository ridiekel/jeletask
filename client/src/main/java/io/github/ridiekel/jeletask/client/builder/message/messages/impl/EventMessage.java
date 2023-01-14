package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.FunctionConfigurable;
import io.github.ridiekel.jeletask.client.builder.message.messages.FunctionBasedMessageSupport;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class EventMessage extends FunctionBasedMessageSupport {
    private final int number;
    private final ComponentState state;
    private final byte[] rawBytes;

    private final byte[] stateBytes;

    public EventMessage(CentralUnit centralUnit, byte[] rawBytes, Function function, int number, ComponentState state) {
        super(centralUnit, function);
        this.rawBytes = rawBytes;
        this.number = number;
        this.state = state;
        this.stateBytes = ArrayUtils.subarray(rawBytes, 8, rawBytes.length-1);
    }

    public ComponentState getState() {
        return this.state;
    }

    public int getNumber() {
        return this.number;
    }

    public byte[] getRawBytes() {
        return this.rawBytes;
    }

    @Override
    protected byte[] getPayload() {
        FunctionConfigurable functionConfig = this.getMessageHandler().getFunctionConfig(this.getFunction());
        byte[] function = {(byte) functionConfig.getNumber()};
        byte[] output = this.getMessageHandler().composeOutput(this.getNumber());
        return Bytes.concat(function, output);
    }

    @Override
    protected Command getCommand() {
        return Command.EVENT;
    }

    @Override
    protected String[] getPayloadLogInfo() {
        return new String[]{this.formatFunction(this.getFunction()), this.formatOutput(this.getNumber()), this.formatState(this.stateBytes, this.getState())};
    }

    @Override
    protected String getId() {
        return "EVENT " + super.getId() + "(" + this.number + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        EventMessage that = (EventMessage) o;

        return new EqualsBuilder().appendSuper(super.equals(o)).append(number, that.number).append(state, that.state).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(number).append(state).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("function", this.getFunction())
                .append("number", number)
                .append("state", state)
                .toString();
    }
}
