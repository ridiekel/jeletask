package io.github.ridiekel.jeletask.client.builder.composer;

import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.FunctionConfigurable;
import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.EventMessage;
import io.github.ridiekel.jeletask.client.builder.message.strategy.GroupGetStrategy;
import io.github.ridiekel.jeletask.client.builder.message.strategy.KeepAliveStrategy;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

import java.util.List;

public interface MessageHandler {
    byte[] compose(Command command, byte[] payload);

    CommandConfigurable<?> getCommandConfig(Command command);

    FunctionConfigurable getFunctionConfig(Function function);

    byte[] composeOutput(int... number);

    int getStxValue();

    EventMessage parseEvent(CentralUnit centralUnit, byte[] message);

    Function getFunction(int function);

    Command getCommand(int command);

    boolean knows(Command command);

    boolean knows(Function function);

    String getOutputLogHeaderName(int index);

    KeepAliveStrategy getKeepAliveStrategy();

    int getAcknowledgeValue();

    GroupGetStrategy getGroupGetStrategy();

    MessageSupport parse(CentralUnit centralUnit, byte[] message);

    int getOutputByteSize();

    EventMessage createResponseEventMessage(CentralUnit centralUnit, Function function, OutputState outputState);

    int getLogStateByte(ComponentState state);

    record OutputState(int number, ComponentState state) {
    }
}
