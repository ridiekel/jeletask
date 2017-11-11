package io.jeletask.teletask.client.builder.composer.config.configurables.command;

import io.jeletask.teletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.jeletask.teletask.client.builder.message.messages.impl.EventMessage;
import io.jeletask.teletask.model.spec.Command;

public abstract class EventCommandConfigurable extends CommandConfigurable<EventMessage> {
    public EventCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.EVENT, number, needsCentralUnitParameter, paramNames);
    }
}
