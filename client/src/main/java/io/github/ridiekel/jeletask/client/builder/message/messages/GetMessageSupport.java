package io.github.ridiekel.jeletask.client.builder.message.messages;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.EventMessage;
import io.github.ridiekel.jeletask.model.spec.CentralUnit;
import io.github.ridiekel.jeletask.model.spec.Command;
import io.github.ridiekel.jeletask.model.spec.ComponentSpec;
import io.github.ridiekel.jeletask.model.spec.Function;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class GetMessageSupport extends FunctionBasedMessageSupport {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(GetMessageSupport.class);

    private final int[] numbers;

    protected GetMessageSupport(Function function, CentralUnit clientConfig, int... numbers) {
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
    public List<EventMessage> respond(CentralUnit config, MessageHandler messageHandler) {
        Collection<MessageHandler.OutputState> states = new ArrayList<>();
        for (int number : this.getNumbers()) {

            ComponentSpec component = config.getComponent(this.getFunction(), number);

            if (component != null) {
                if (component.getState() == null) {
                    component.setState(messageHandler.getFunctionConfig(this.getFunction()).getStateCalculator().getDefaultState(component));
                }

                states.add(new MessageHandler.OutputState(number, component.getState()));
            } else {
                LOG.debug("Component {}:{} not found.", this.getFunction(), number);
            }
        }
        return messageHandler.createResponseEventMessage(config, this.getFunction(), states.stream().toArray(MessageHandler.OutputState[]::new));
    }

    @Override
    protected String getId() {
        return "GET " + super.getId() + "(" + Arrays.stream(this.numbers).mapToObj(String::valueOf).collect(Collectors.joining(",")) + ")";
    }
}
