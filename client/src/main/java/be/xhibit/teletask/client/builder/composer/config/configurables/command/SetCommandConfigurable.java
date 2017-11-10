package be.xhibit.teletask.client.builder.composer.config.configurables.command;

import be.xhibit.teletask.client.builder.composer.config.configurables.CommandConfigurable;
import be.xhibit.teletask.client.builder.message.messages.impl.SetMessage;
import be.xhibit.teletask.model.spec.Command;

public abstract class SetCommandConfigurable extends CommandConfigurable<SetMessage> {
    public SetCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.SET, number, needsCentralUnitParameter, paramNames);
    }
}
