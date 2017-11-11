package io.jeletask.teletask.client.builder.message.messages;

import io.jeletask.teletask.client.builder.composer.MessageHandler;
import io.jeletask.teletask.client.builder.message.messages.impl.EventMessage;
import io.jeletask.teletask.model.spec.ClientConfigSpec;
import io.jeletask.teletask.model.spec.Command;
import io.jeletask.teletask.model.spec.ComponentSpec;
import io.jeletask.teletask.model.spec.Function;
import io.jeletask.teletask.utilities.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class GetMessageSupport extends FunctionBasedMessageSupport {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(GetMessageSupport.class);

    private final int[] numbers;

    protected GetMessageSupport(Function function, ClientConfigSpec clientConfig, int... numbers) {
        super(clientConfig, function);
        this.numbers = numbers;
    }

    public int[] getNumbers() {
        return this.numbers;
    }

    @Override
    protected byte[] getPayload() {
        return Bytes.concat(new byte[]{(byte) this.getMessageHandler().getFunctionConfig(this.getFunction()).getNumber()}, this.getMessageHandler().composeOutput(this.getNumbers()));
    }

    @Override
    public Command getCommand() {
        return Command.GET;
    }

    @Override
    protected String[] getPayloadLogInfo() {
        return new String[]{this.formatFunction(this.getFunction()), this.formatOutput(this.getNumbers())};
    }

    @Override
    public List<EventMessage> respond(ClientConfigSpec config, MessageHandler messageHandler) {
        Collection<MessageHandler.OutputState> states = new ArrayList<>();
        for (int number : this.getNumbers()) {

            ComponentSpec component = config.getComponent(this.getFunction(), number);

            if (component != null) {
                if(component.getState() == null) {
                    component.setState(messageHandler.getFunctionConfig(this.getFunction()).getStateCalculator().getDefaultState(component));
                }

                states.add(new MessageHandler.OutputState(number, component.getState()));
            } else {
                LOG.debug("Component {}:{} not found.", this.getFunction(), number);
            }
        }
        return messageHandler.createResponseEventMessage(config, this.getFunction(), states.stream().toArray(MessageHandler.OutputState[]::new));
    }

}
