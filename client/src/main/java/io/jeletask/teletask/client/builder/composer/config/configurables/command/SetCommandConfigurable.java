package io.jeletask.teletask.client.builder.composer.config.configurables.command;

import io.jeletask.teletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.jeletask.teletask.client.builder.message.messages.impl.SetMessage;
import io.jeletask.teletask.model.spec.Command;

public abstract class SetCommandConfigurable extends CommandConfigurable<SetMessage> {
    public SetCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.SET, number, needsCentralUnitParameter, paramNames);
    }
}
