package io.jeletask.client.builder.message.messages.impl;

import io.jeletask.client.builder.message.messages.MessageSupport;
import io.jeletask.model.spec.ClientConfigSpec;
import io.jeletask.model.spec.Command;

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
