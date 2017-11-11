package io.jeletask.client.builder.composer.config.configurables.command;

import io.jeletask.client.builder.composer.MessageHandler;
import io.jeletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.jeletask.client.builder.message.messages.impl.KeepAliveMessage;
import io.jeletask.model.spec.ClientConfigSpec;
import io.jeletask.model.spec.Command;

public class KeepAliveCommandConfigurable extends CommandConfigurable<KeepAliveMessage> {
    public KeepAliveCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.KEEP_ALIVE, number, needsCentralUnitParameter, paramNames);
    }

    @Override
    public KeepAliveMessage parse(ClientConfigSpec config, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
        return new KeepAliveMessage(config);
    }
}
