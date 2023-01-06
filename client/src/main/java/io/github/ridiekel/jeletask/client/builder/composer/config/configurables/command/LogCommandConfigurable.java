package io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.LogMessage;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

public class LogCommandConfigurable extends CommandConfigurable<LogMessage> {
    public LogCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.LOG, number, needsCentralUnitParameter, paramNames);
    }

    @Override
    public LogMessage parse(CentralUnit centralUnit, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
        Function function = messageHandler.getFunction(payload[0]);
        return new LogMessage(centralUnit, function, new ComponentState(payload[1] == 0 ? "OFF" : "ON"));
    }
}
