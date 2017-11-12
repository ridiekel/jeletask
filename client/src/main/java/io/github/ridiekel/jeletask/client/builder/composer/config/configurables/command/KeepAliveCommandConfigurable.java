package io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.KeepAliveMessage;
import io.github.ridiekel.jeletask.model.spec.CentralUnit;
import io.github.ridiekel.jeletask.model.spec.Command;

public class KeepAliveCommandConfigurable extends CommandConfigurable<KeepAliveMessage> {
    public KeepAliveCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.KEEP_ALIVE, number, needsCentralUnitParameter, paramNames);
    }

    @Override
    public KeepAliveMessage parse(CentralUnit config, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
        return new KeepAliveMessage(config);
    }
}
