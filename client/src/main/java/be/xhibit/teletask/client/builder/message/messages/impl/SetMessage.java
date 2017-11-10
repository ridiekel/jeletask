package be.xhibit.teletask.client.builder.message.messages.impl;

import be.xhibit.teletask.client.TeletaskClient;
import be.xhibit.teletask.client.builder.composer.MessageHandler;
import be.xhibit.teletask.client.builder.composer.MessageHandlerFactory;
import be.xhibit.teletask.client.builder.composer.config.configurables.FunctionConfigurable;
import be.xhibit.teletask.client.builder.message.MessageUtilities;
import be.xhibit.teletask.client.builder.message.messages.FunctionStateBasedMessageSupport;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;
import be.xhibit.teletask.model.spec.ComponentSpec;
import be.xhibit.teletask.model.spec.Function;
import com.google.common.primitives.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SetMessage extends FunctionStateBasedMessageSupport {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SetMessage.class);

    private final int number;

    public SetMessage(ClientConfigSpec clientConfig, Function function, int number, String state) {
        super(clientConfig, function, state);
        this.number = number;
    }

    public int getNumber() {
        return this.number;
    }

    @Override
    protected byte[] getPayload() {
        FunctionConfigurable functionConfig = this.getMessageHandler().getFunctionConfig(this.getFunction());
        ComponentSpec component = this.getClientConfig().getComponent(this.getFunction(), this.getNumber());
        byte[] function = {(byte) functionConfig.getNumber()};
        byte[] output = MessageHandlerFactory.getMessageHandler(this.getClientConfig().getCentralUnitType()).composeOutput(this.getNumber());
        byte[] state = functionConfig.getStateCalculator().convertSet(component, this.getState());
        return Bytes.concat(function, output, state);
    }

    @Override
    public void execute(TeletaskClient client) {
        super.execute(client);

        ComponentSpec component = this.getClientConfig().getComponent(this.getFunction(), this.getNumber());
        String initialState = component.getState();
        Long start = System.currentTimeMillis();
        while (!this.getState().equals(component.getState()) && (System.currentTimeMillis() - start) < 5000) {
            try {
                Thread.sleep(10);

            } catch (InterruptedException e) {
                LOG.error("Exception ({}) caught in set: {}", e.getClass().getName(), e.getMessage(), e);
            }
            try {
                client.handleReceiveEvents(MessageUtilities.receive(LOG, client));
            } catch (Exception e) {
                LOG.error("Exception ({}) caught in execute: {}", e.getClass().getName(), e.getMessage(), e);
            }
        }
        if (!this.getState().equals(component.getState())) {
            String message = "Did not receive a state change for " + component.getFunction() + ":" + component.getNumber() + " ("+component.getDescription()+") within 5 seconds. Assuming failed to set state from '" + initialState + "' to '" + this.getState() + "'";
            LOG.warn(message);
            throw new RuntimeException(message);
        }
    }

    @Override
    protected Command getCommand() {
        return Command.SET;
    }

    @Override
    protected String[] getPayloadLogInfo() {
        return new String[]{this.formatFunction(this.getFunction()), this.formatOutput(this.getNumber()), this.formatState(this.getFunction(), this.getNumber(), this.getState())};
    }

    @Override
    public List<EventMessage> respond(ClientConfigSpec config, MessageHandler messageHandler) {
        return messageHandler.createResponseEventMessage(config, this.getFunction(), new MessageHandler.OutputState(this.getNumber(), this.getState()));
    }

}
