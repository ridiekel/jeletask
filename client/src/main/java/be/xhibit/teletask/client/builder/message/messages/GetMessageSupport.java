package be.xhibit.teletask.client.builder.message.messages;

import be.xhibit.teletask.client.builder.composer.MessageHandler;
import be.xhibit.teletask.client.builder.message.messages.impl.EventMessage;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;
import be.xhibit.teletask.model.spec.ComponentSpec;
import be.xhibit.teletask.model.spec.Function;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Bytes;
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
        return messageHandler.createResponseEventMessage(config, this.getFunction(), Iterables.toArray(states, MessageHandler.OutputState.class));
    }

}
