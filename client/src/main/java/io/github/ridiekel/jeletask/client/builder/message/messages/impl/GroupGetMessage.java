package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.builder.message.messages.GetMessageSupport;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.Function;

public class GroupGetMessage extends GetMessageSupport {
    public GroupGetMessage(CentralUnit centralUnit, Function function, int... number) {
        super(function, centralUnit, number);
    }

    @Override
    public Command getCommand() {
        return Command.GROUPGET;
    }

    @Override
    protected String getLogHeaderName(int index) {
        String name = super.getLogHeaderName(index);
        return name == null ? this.getMessageHandler().getOutputLogHeaderName(index) : name;
    }

    @Override
    protected String getId() {
        return "GROUP" + super.getId();
    }
}
