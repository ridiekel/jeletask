package io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command;

import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.SetMessage;
import io.github.ridiekel.jeletask.client.spec.Command;

public abstract class SetCommandConfigurable extends CommandConfigurable<SetMessage> {
    public SetCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.SET, number, needsCentralUnitParameter, paramNames);
    }
}
