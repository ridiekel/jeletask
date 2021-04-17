package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.FunctionConfigurable;
import io.github.ridiekel.jeletask.client.builder.message.messages.FunctionBasedMessageSupport;
import io.github.ridiekel.jeletask.model.spec.CentralUnit;
import io.github.ridiekel.jeletask.model.spec.Command;
import io.github.ridiekel.jeletask.model.spec.ComponentSpec;
import io.github.ridiekel.jeletask.model.spec.Function;
import io.github.ridiekel.jeletask.utilities.Bytes;

public class EventMessage extends FunctionBasedMessageSupport {
    private final int number;
    private final String state;
    private final byte[] rawBytes;

    public EventMessage(CentralUnit clientConfig, byte[] rawBytes, Function function, int number, String state) {
        super(clientConfig, function);
        this.rawBytes = rawBytes;
        this.number = number;
        this.state = state;
    }

    public String getState() {
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
        ComponentSpec component = this.getClientConfig().getComponent(this.getFunction(), this.getNumber());
        byte[] function = {(byte) functionConfig.getNumber()};
        byte[] output = this.getMessageHandler().composeOutput(this.getNumber());
        byte[] state = functionConfig.getStateCalculator().convertSet(component, this.getState());
        return Bytes.concat(function, output, state);
    }

    @Override
    protected Command getCommand() {
        return Command.EVENT;
    }

    @Override
    protected String[] getPayloadLogInfo() {
        return new String[]{this.formatFunction(this.getFunction()), this.formatOutput(this.getNumber()), this.formatState(this.getFunction(), this.getNumber(), this.getState())};
    }

    @Override
    protected String getId() {
        return "EVENT " + super.getId() + "(" + this.number + ")";
    }
}
