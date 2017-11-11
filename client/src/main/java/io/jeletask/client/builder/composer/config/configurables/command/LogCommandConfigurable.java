package io.jeletask.client.builder.composer.config.configurables.command;

import io.jeletask.client.builder.composer.MessageHandler;
import io.jeletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.jeletask.client.builder.message.messages.impl.LogMessage;
import io.jeletask.model.spec.ClientConfigSpec;
import io.jeletask.model.spec.Command;
import io.jeletask.model.spec.Function;

public class LogCommandConfigurable extends CommandConfigurable<LogMessage> {
    public LogCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.LOG, number, needsCentralUnitParameter, paramNames);
    }

    @Override
    public LogMessage parse(ClientConfigSpec config, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
        Function function = messageHandler.getFunction(payload[0]);
        return new LogMessage(config, function, payload[1] == 0 ? "OFF" : "ON");
    }
}
