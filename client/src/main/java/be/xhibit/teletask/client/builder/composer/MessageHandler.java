package be.xhibit.teletask.client.builder.composer;

import be.xhibit.teletask.client.builder.composer.config.configurables.CommandConfigurable;
import be.xhibit.teletask.client.builder.composer.config.configurables.FunctionConfigurable;
import be.xhibit.teletask.client.builder.message.messages.MessageSupport;
import be.xhibit.teletask.client.builder.message.messages.impl.EventMessage;
import be.xhibit.teletask.client.builder.message.strategy.GroupGetStrategy;
import be.xhibit.teletask.client.builder.message.strategy.KeepAliveStrategy;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;
import be.xhibit.teletask.model.spec.Function;

import java.util.List;

public interface MessageHandler {
    byte[] compose(Command command, byte[] payload);

    CommandConfigurable<?> getCommandConfig(Command command);

    FunctionConfigurable getFunctionConfig(Function function);

    byte[] composeOutput(int... number);

    int getStxValue();

    EventMessage parseEvent(ClientConfigSpec config, byte[] message);

    Function getFunction(int function);

    Command getCommand(int command);

    boolean knows(Command command);

    boolean knows(Function function);

    String getOutputLogHeaderName(int index);

    KeepAliveStrategy getKeepAliveStrategy();

    int getAcknowledgeValue();

    GroupGetStrategy getGroupGetStrategy();

    MessageSupport parse(ClientConfigSpec config, byte[] message);

    int getOutputByteSize();

    List<EventMessage> createResponseEventMessage(ClientConfigSpec config, Function function, OutputState... numbers);

    int getLogStateByte(String state);

    class OutputState {
        private final int number;
        private final String state;

        public OutputState(int number, String state) {
            this.number = number;
            this.state = state;
        }

        public int getNumber() {
            return this.number;
        }

        public String getState() {
            return this.state;
        }
    }
}
