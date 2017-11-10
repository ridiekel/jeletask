package be.xhibit.teletask.client.builder.message.messages.impl;

import be.xhibit.teletask.client.builder.message.messages.GetMessageSupport;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;
import be.xhibit.teletask.model.spec.Function;

public class GroupGetMessage extends GetMessageSupport {
    public GroupGetMessage(ClientConfigSpec clientConfig, Function function, int... number) {
        super(function, clientConfig, number);
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

}
