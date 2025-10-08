package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.builder.message.messages.FunctionStateBasedMessageSupport;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.impl.LogState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class LogMessage extends FunctionStateBasedMessageSupport<LogState> {
    private static final Logger LOG = LogManager.getLogger();

    public LogMessage(CentralUnit ClientConfig, Function function, LogState state) {
        super(ClientConfig, function, state);
    }

    @Override
    protected byte[] getPayload() {
        return new byte[]{(byte) this.getMessageHandler().getFunctionConfig(this.getFunction()).getNumber(), (byte) this.getLogStateByte(this.getState())};
    }

    @Override
    protected Command getCommand() {
        return Command.LOG;
    }

    @Override
    protected String[] getPayloadLogInfo() {
        return new String[]{this.formatFunction(this.getFunction()), this.formatState(this.getState())};
    }

    protected String formatState(LogState... states) {
        return Arrays.stream(states)
                .map(state -> "State: " + (state == null ? null : this.getLogStateByte(state)) + " | " + (state == null ? null : Bytes.bytesToHex((byte) this.getLogStateByte(state))) + (LOG.isTraceEnabled() ? "\n" : " ") + Optional.ofNullable(state).map(s -> LOG.isTraceEnabled() ? s.prettyString() : s.toString()).orElse(null))
                .collect(Collectors.joining(", "));
    }

    @Override
    protected String toLogLine(String message) {
        return String.format("[%s] - [%s] - [%s] - %s",
                StringUtils.rightPad(message, 10),
                StringUtils.rightPad(this.getFunction().toString(), 10),
                StringUtils.rightPad(this.getCommand().toString(), 10),
                this.getState()
        );
    }


    public int getLogStateByte(io.github.ridiekel.jeletask.client.spec.state.impl.LogState state) {
        return io.github.ridiekel.jeletask.client.builder.composer.LogState.valueOf(state.getState().name()).getByteValue();
    }

    @Override
    protected String getId() {
        return "LOG " + super.getId();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("function", this.getFunction())
                .append("state", this.getState())
                .toString();
    }
}
