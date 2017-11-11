package io.jeletask.client.builder.composer.config.configurables.command;

import io.jeletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.jeletask.client.builder.message.messages.impl.EventMessage;
import io.jeletask.model.spec.Command;

public abstract class EventCommandConfigurable extends CommandConfigurable<EventMessage> {
    public EventCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.EVENT, number, needsCentralUnitParameter, paramNames);
    }
}
