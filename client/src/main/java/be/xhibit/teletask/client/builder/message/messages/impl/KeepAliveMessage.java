package be.xhibit.teletask.client.builder.message.messages.impl;

import be.xhibit.teletask.client.builder.message.messages.MessageSupport;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;

public class KeepAliveMessage extends MessageSupport {
    public KeepAliveMessage(ClientConfigSpec clientConfig) {
        super(clientConfig);
    }

    @Override
    protected byte[] getPayload() {
        return new byte[0];
    }

    @Override
    public Command getCommand() {
        return Command.KEEP_ALIVE;
    }

    @Override
    protected String[] getPayloadLogInfo() {
        return new String[]{"None"};
    }

}
