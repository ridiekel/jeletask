package io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.DisplayMessage;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;

public class DisplayMessageCommandConfigurable extends CommandConfigurable<DisplayMessage> {

    public DisplayMessageCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.DISPLAYMESSAGE, number, needsCentralUnitParameter, paramNames);
    }

    @Override
    public DisplayMessage parse(CentralUnit config, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
        // Not implemented. Not needed for DisplayMessage?
        return null;
    }
}
