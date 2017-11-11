package be.xhibit.teletask.client.builder.message.messages.impl;

import be.xhibit.teletask.client.builder.ByteUtilities;
import be.xhibit.teletask.client.builder.message.messages.FunctionStateBasedMessageSupport;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;
import be.xhibit.teletask.model.spec.Function;

import java.util.Arrays;
import java.util.stream.Collectors;

public class LogMessage extends FunctionStateBasedMessageSupport {
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
        return Arrays.stream(states)
                .map(state -> "State: " + state + " | " + (state == null ? null : this.getMessageHandler().getLogStateByte(state)) + " | " + (state == null ? null : ByteUtilities.bytesToHex((byte) this.getMessageHandler().getLogStateByte(state))))
                .collect(Collectors.joining(", "));
    }

    @Override
    protected boolean isValid() {
        return true;
    }
}
