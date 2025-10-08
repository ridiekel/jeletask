package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.TeletaskClientImpl;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.DisplayMessageStateCalculator;
import io.github.ridiekel.jeletask.client.builder.message.messages.AcknowledgeException;
import io.github.ridiekel.jeletask.client.builder.message.messages.FunctionStateBasedMessageSupport;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.impl.DisplayMessageState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class DisplayMessage extends FunctionStateBasedMessageSupport<DisplayMessageState> {
    private final int number;

    private final byte[] bus_and_address_bytes;
    private final byte[] displaymessage_bytes;

    public DisplayMessage(CentralUnit centralUnit, Function function, int number, DisplayMessageState state) {
        super(centralUnit, function, state);
        this.number = number;

        ComponentSpec component = this.getCentralUnit().getComponent(this.getFunction(), this.getNumber());

        DisplayMessageStateCalculator displayMessageStateCalculator = this.getMessageHandler().getFunctionConfig(this.getFunction()).getStateCalculator(component);

        if (component.getAddressNumbers() == null || component.getBusNumbers() == null)
            throw new IllegalArgumentException("Address or bus numbers are missing");

        String[] busNumbers = component.getBusNumbers().split(",");
        String[] addressNumbers = component.getAddressNumbers().split(",");

        if (addressNumbers.length != busNumbers.length)
            throw new IllegalArgumentException("Address and bus numbers configuration error");

        byte[] busNumbers_bytes = Bytes.EMPTY;
        for(String busNumber: busNumbers)
            busNumbers_bytes = Bytes.concat(busNumbers_bytes, new byte[]{Byte.valueOf(busNumber)});

        byte[] addressNumbers_bytes = Bytes.EMPTY;
        for(String addressNumber: addressNumbers)
            addressNumbers_bytes = Bytes.concat(addressNumbers_bytes, new byte[]{Byte.valueOf(addressNumber)});

        this.bus_and_address_bytes = Bytes.concat(busNumbers_bytes, addressNumbers_bytes);
        this.displaymessage_bytes = displayMessageStateCalculator.toCommand(this.getState());
    }

    @Override
    protected String toLogLine(String message) {
        return String.format("[%s] - [%s] - [%s] - [%s] - [%s] - %s",
                StringUtils.rightPad(message, 10),
                StringUtils.rightPad(this.getFunction().toString(), 10),
                StringUtils.leftPad(String.valueOf(this.getNumber()), 3),
                StringUtils.leftPad(this.getCentralUnit().getComponent(getFunction(), number).getDescription(), 40),
                StringUtils.rightPad(this.getCommand().toString(), 10),
                this.getState()
        );
    }

    public int getNumber() {
        return this.number;
    }

    @Override
    protected byte[] getPayload() {
        return Bytes.concat(bus_and_address_bytes, displaymessage_bytes);
    }

    @Override
    public void execute(TeletaskClientImpl client) throws AcknowledgeException {
        super.execute(client);

    }

    @Override
    protected Command getCommand() {
        return Command.DISPLAYMESSAGE;
    }

    @Override
    protected String[] getPayloadLogInfo() {
        return new String[]{
                this.formatFunction(this.getFunction()),
                this.formatOutput(this.getNumber()),
                this.formatState(this.displaymessage_bytes, this.getState())
        };
    }

    @Override
    protected String getId() {
        return "DISPLAYMESSAGE " + super.getId() + "(" + this.number + ")";
    }

    @Override
    protected boolean isValid() {
        return this.getMessageHandler().getFunctionConfig(this.getFunction()).<DisplayMessageStateCalculator>getStateCalculator(getCentralUnit().getComponent(getFunction(), number)).isValidWriteState(this.getState());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        DisplayMessage that = (DisplayMessage) o;

        return new EqualsBuilder().appendSuper(super.equals(o)).append(number, that.number).isEquals();
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
