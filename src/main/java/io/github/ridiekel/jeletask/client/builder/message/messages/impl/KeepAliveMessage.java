package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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
    protected String toLogLine(String message) {
        return String.format("[%s] - [%s]",
                StringUtils.rightPad(message, 10),
                StringUtils.rightPad(this.getCommand().toString(), 10)
        );
    }

    @Override
    protected String getId() {
        return "KEEPALIVE";
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .toString();
    }
}
