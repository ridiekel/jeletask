package io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command;

import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.EventMessage;
import io.github.ridiekel.jeletask.client.spec.Command;

public abstract class EventCommandConfigurable extends CommandConfigurable<EventMessage> {
    public EventCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.EVENT, number, needsCentralUnitParameter, paramNames);
    }
}
