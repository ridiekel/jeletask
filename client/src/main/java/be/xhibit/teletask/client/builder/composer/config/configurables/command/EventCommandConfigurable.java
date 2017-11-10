package be.xhibit.teletask.client.builder.composer.config.configurables.command;

import be.xhibit.teletask.client.builder.composer.config.configurables.CommandConfigurable;
import be.xhibit.teletask.client.builder.message.messages.impl.EventMessage;
import be.xhibit.teletask.model.spec.Command;

public abstract class EventCommandConfigurable extends CommandConfigurable<EventMessage> {
    public EventCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.EVENT, number, needsCentralUnitParameter, paramNames);
    }
}
