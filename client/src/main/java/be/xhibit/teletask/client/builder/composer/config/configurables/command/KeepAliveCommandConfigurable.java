package be.xhibit.teletask.client.builder.composer.config.configurables.command;

import be.xhibit.teletask.client.builder.composer.MessageHandler;
import be.xhibit.teletask.client.builder.composer.config.configurables.CommandConfigurable;
import be.xhibit.teletask.client.builder.message.messages.impl.KeepAliveMessage;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;

public class KeepAliveCommandConfigurable extends CommandConfigurable<KeepAliveMessage> {
    public KeepAliveCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.KEEP_ALIVE, number, needsCentralUnitParameter, paramNames);
    }

    @Override
    public KeepAliveMessage parse(ClientConfigSpec config, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
        return new KeepAliveMessage(config);
    }
}
