package io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command;

import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.GetMessage;
import io.github.ridiekel.jeletask.model.spec.Command;

public abstract class GetCommandConfigurable extends CommandConfigurable<GetMessage> {
    public GetCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.GET, number, needsCentralUnitParameter, paramNames);
    }
}
