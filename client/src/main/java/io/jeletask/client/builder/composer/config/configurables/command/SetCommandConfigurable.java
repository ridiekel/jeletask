package io.jeletask.client.builder.composer.config.configurables.command;

import io.jeletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.jeletask.client.builder.message.messages.impl.SetMessage;
import io.jeletask.model.spec.Command;

public abstract class SetCommandConfigurable extends CommandConfigurable<SetMessage> {
    public SetCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.SET, number, needsCentralUnitParameter, paramNames);
    }
}
