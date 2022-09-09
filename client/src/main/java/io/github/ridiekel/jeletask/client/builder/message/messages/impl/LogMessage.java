package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.builder.message.messages.FunctionStateBasedMessageSupport;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.utilities.Bytes;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class LogMessage extends FunctionStateBasedMessageSupport {
    public LogMessage(CentralUnit ClientConfig, Function function, ComponentState state) {
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

    protected String formatState(ComponentState... states) {
        return Arrays.stream(states)
                .map(state -> "State: " + (state == null ? null : this.getMessageHandler().getLogStateByte(state)) + " | " + (state == null ? null : Bytes.bytesToHex((byte) this.getMessageHandler().getLogStateByte(state))) + "\n" + Optional.ofNullable(state).map(ComponentState::prettyString).orElse(null))
                .collect(Collectors.joining(", "));
    }

    @Override
    protected boolean isValid() {
        return true;
    }

    @Override
    protected String getId() {
        return "LOG " + super.getId();
    }
}
