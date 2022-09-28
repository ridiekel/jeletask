package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.TeletaskClientImpl;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.FunctionConfigurable;
import io.github.ridiekel.jeletask.client.builder.message.messages.AcknowledgeException;
import io.github.ridiekel.jeletask.client.builder.message.messages.FunctionStateBasedMessageSupport;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DisplayMessage extends FunctionStateBasedMessageSupport {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DisplayMessage.class);

    private final int number;

    private final byte[] bus_and_address_bytes;
    private final byte[] displaymessage_bytes;

    public DisplayMessage(CentralUnit clientConfig, Function function, int number, ComponentState state) {
        super(clientConfig, function, state);
        this.number = number;

        FunctionConfigurable functionConfig = this.getMessageHandler().getFunctionConfig(this.getFunction());
        ComponentSpec component = this.getClientConfig().getComponent(this.getFunction(), this.getNumber());

        if (component.getAddressNumbers() == null || component.getBusNumbers() == null)
            throw new IllegalArgumentException("Address or bus numbers are missing");

        String busNumbers[] = component.getBusNumbers().split(",");
        String addressNumbers[] = component.getAddressNumbers().split(",");

        if (addressNumbers.length != busNumbers.length)
            throw new IllegalArgumentException("Address and bus numbers configuration error");

        byte[] busNumbers_bytes = Bytes.EMPTY;
        for(String busNumber: busNumbers)
            busNumbers_bytes = Bytes.concat(busNumbers_bytes, new byte[]{Byte.valueOf(busNumber)});

        byte[] addressNumbers_bytes = Bytes.EMPTY;
        for(String addressNumber: addressNumbers)
            addressNumbers_bytes = Bytes.concat(addressNumbers_bytes, new byte[]{Byte.valueOf(addressNumber)});

        this.bus_and_address_bytes = Bytes.concat(busNumbers_bytes, addressNumbers_bytes);
        this.displaymessage_bytes = functionConfig.getStateCalculator(component).toBytes(this.getState());
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
    public List<EventMessage> respond(CentralUnit config, MessageHandler messageHandler) {
        return messageHandler.createResponseEventMessage(config, this.getFunction(), new MessageHandler.OutputState(this.getNumber(), this.getState()));
    }

    @Override
    protected String getId() {
        return "DISPLAYMESSAGE " + super.getId() + "(" + this.number + ")";
    }

    @Override
    protected boolean isValid() {
        FunctionConfigurable functionConfig = this.getMessageHandler().getFunctionConfig(this.getFunction());
        return functionConfig.isValidState(this.getClientConfig().getComponent(this.getFunction(), this.getNumber()), this.getState());
    }
}
