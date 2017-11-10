package be.xhibit.teletask.client.builder.message.messages.impl;

import be.xhibit.teletask.client.builder.ByteUtilities;
import be.xhibit.teletask.client.builder.message.messages.FunctionStateBasedMessageSupport;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;
import be.xhibit.teletask.model.spec.Function;
import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

public class LogMessage extends FunctionStateBasedMessageSupport {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(LogMessage.class);

    public LogMessage(ClientConfigSpec ClientConfig, Function function, String state) {
        super(ClientConfig, function, state);
    }

    @Override
    protected byte[] getPayload() {
        return new byte[]{(byte) this.getMessageHandler().getFunctionConfig(this.getFunction()).getNumber(), (byte) this.getMessageHandler().getLogStateByte(this.getState())};
    }

    @Override
    protected Command getCommand() {
        return Command.LOG;
    }

    @Override
    protected String[] getPayloadLogInfo() {
        return new String[]{this.formatFunction(this.getFunction()), this.formatState(this.getState())};
    }

    protected String formatState(String... states) {
        Collection<String> log = new ArrayList<>();
        for (String state : states) {
            log.add("State: " + state + " | " + (state == null ? null : this.getMessageHandler().getLogStateByte(state)) + " | " + (state == null ? null : ByteUtilities.bytesToHex((byte) this.getMessageHandler().getLogStateByte(state))));
        }
        return Joiner.on(", ").join(log);
    }

    @Override
    protected boolean isValid() {
        return true;
    }
}
