package be.xhibit.teletask.client.builder.composer.config.configurables.command;

import be.xhibit.teletask.client.builder.composer.MessageHandler;
import be.xhibit.teletask.client.builder.composer.config.configurables.CommandConfigurable;
import be.xhibit.teletask.client.builder.message.messages.impl.LogMessage;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;
import be.xhibit.teletask.model.spec.Function;

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
