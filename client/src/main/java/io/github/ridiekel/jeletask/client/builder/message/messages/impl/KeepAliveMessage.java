package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;

public class KeepAliveMessage extends MessageSupport {
    public KeepAliveMessage(CentralUnit centralUnit) {
        super(centralUnit);
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

    @Override
    protected String getId() {
        return "KEEPALIVE";
    }

}
