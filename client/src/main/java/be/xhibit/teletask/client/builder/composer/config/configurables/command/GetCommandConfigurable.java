package be.xhibit.teletask.client.builder.composer.config.configurables.command;

import be.xhibit.teletask.client.builder.composer.config.configurables.CommandConfigurable;
import be.xhibit.teletask.client.builder.message.messages.impl.GetMessage;
import be.xhibit.teletask.model.spec.Command;

public abstract class GetCommandConfigurable extends CommandConfigurable<GetMessage> {
    public GetCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.GET, number, needsCentralUnitParameter, paramNames);
    }
}
