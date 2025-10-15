package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandlerFactory;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.FunctionConfigurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.StateCalculator;
import io.github.ridiekel.jeletask.client.builder.message.messages.FunctionStateBasedMessageSupport;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.utilities.Bytes;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class SetMessage extends FunctionStateBasedMessageSupport<State<?>> {

    @Getter
    private final int number;

    private byte[] functionBytes;
    private byte[] outputBytes;
    private byte[] stateBytes;

    public SetMessage(CentralUnit centralUnit, Function function, int number, State<?> state) {
        super(centralUnit, function, state);
        this.number = number;
    }

    private void init() {
        if (this.functionBytes == null) {
            FunctionConfigurable functionConfig = this.getMessageHandler().getFunctionConfig(this.getFunction());
            ComponentSpec component = this.getCentralUnit().getComponent(this.getFunction(), this.getNumber());
            this.functionBytes = new byte[]{(byte) functionConfig.getNumber()};
            this.outputBytes = MessageHandlerFactory.getMessageHandler(this.getCentralUnit().getCentralUnitType()).composeOutput(this.getNumber());
            this.stateBytes = functionConfig.<StateCalculator<State<?>>>getStateCalculator(component).toCommand(this.getState());
        }
    }

    @Override
    protected String toLogLine(String message) {
        return String.format("[%s] - [%s] - [%s] - [%s] - [%s] - %s",
                StringUtils.rightPad(message, 10),
                StringUtils.rightPad(this.getFunction().toString(), 10),
                StringUtils.leftPad(String.valueOf(this.number), 3),
                StringUtils.leftPad(this.getCentralUnit().getComponent(getFunction(), number).getDescription(), 40),
                StringUtils.rightPad(this.getCommand().toString(), 10),
                this.getState()
        );
    }

    public byte[] getFunctionBytes() {
        init();
        return functionBytes;
    }

    public byte[] getOutputBytes() {
        init();
        return outputBytes;
    }

    public byte[] getStateBytes() {
        init();
        return stateBytes;
    }

    @Override
    protected byte[] getPayload() {
        return Bytes.concat(getFunctionBytes(), getOutputBytes(), getStateBytes());
    }

    @Override
    protected Command getCommand() {
        return Command.SET;
    }

    @Override
    protected String[] getPayloadLogInfo() {
        return new String[]{
                this.formatFunction(this.getFunction()),
                this.formatOutput(this.getNumber()),
                this.formatState(this.getStateBytes(), this.getState())
        };
    }

    @Override
    protected String getId() {
        return "SET " + super.getId() + "(" + this.number + ")";
    }

    @Override
    protected boolean isValid() {
        return this.getMessageHandler().getFunctionConfig(this.getFunction()).<StateCalculator<State<?>>>getStateCalculator(getCentralUnit().getComponent(getFunction(), number)).isValidWriteState(this.getState());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        SetMessage that = (SetMessage) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(number, that.number)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(number).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("function", this.getFunction())
                .append("number", number)
                .append("state", this.getState())
                .toString();
    }
}
