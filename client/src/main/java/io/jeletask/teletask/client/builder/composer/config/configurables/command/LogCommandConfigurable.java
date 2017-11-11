package io.jeletask.teletask.client.builder.composer.config.configurables.command;

import io.jeletask.teletask.client.builder.composer.MessageHandler;
import io.jeletask.teletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.jeletask.teletask.client.builder.message.messages.impl.LogMessage;
import io.jeletask.teletask.model.spec.ClientConfigSpec;
import io.jeletask.teletask.model.spec.Command;
import io.jeletask.teletask.model.spec.Function;

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
