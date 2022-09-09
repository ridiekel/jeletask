package io.github.ridiekel.jeletask.client.builder.message.messages;

import io.github.ridiekel.jeletask.client.TeletaskClientImpl;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandlerFactory;
import io.github.ridiekel.jeletask.client.builder.message.MessageUtilities;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.EventMessage;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class MessageSupport {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MessageSupport.class);

    private static final Pattern REMOVE_NAMES = Pattern.compile("[^\\|]");
    private static final Pattern INSERT_PLACEHOLDERS = Pattern.compile("\\|   ");
    public static final int ACK_WAIT_TIME = 2000;

    private final CentralUnit clientConfig;

    private boolean acknowledged = false;

    protected MessageSupport(CentralUnit clientConfig) {
        this.clientConfig = clientConfig;
    }

    public void execute(TeletaskClientImpl client) throws AcknowledgeException {
        MessageHandler messageHandler = this.getMessageHandler();
        if (this.isValid()) {
            if (messageHandler.knows(this.getCommand())) {
                byte[] message = messageHandler.compose(this.getCommand(), this.getPayload());

                LOG.trace("Sending message: {}", this);

                client.send(message, this::getLogInfo);

                LOG.trace("Message sent: {}", this);

                this.waitForAcknowledge(client);

                LOG.trace("Message acknowledged: {}", this);
            } else {
                LOG.warn("Message handler '{}' does not know of command '{}'", this.getMessageHandler().getClass().getSimpleName(), this.getCommand());
            }
        } else {
            LOG.warn("Invalid request: {}", this);
        }
    }

    private void waitForAcknowledge(TeletaskClientImpl client) throws AcknowledgeException {
        try {
            Awaitility.await(String.format("Acknowlegde - %s", this.getId()))
                    .pollInterval(10, TimeUnit.MILLISECONDS)
                    .atMost(ACK_WAIT_TIME, TimeUnit.MILLISECONDS)
                    .pollInSameThread()
                    .until(() -> {
                        try {
                            client.handleReceiveEvents(MessageUtilities.receive(LOG, client, this));
                        } catch (Exception e) {
                            LOG.error("Exception ({}) caught in execute: {}", e.getClass().getName(), e.getMessage(), e);
                        }
                        return this.isAcknowledged();
                    });
        } catch (Exception e) {
            throw new AcknowledgeException(String.format("%s - Did not receive acknowledge from the Teletask Central Unit within %s ms", this.getId(), ACK_WAIT_TIME), e);
        }
    }

    protected boolean isValid() {
        return true;
    }

    protected CentralUnit getClientConfig() {
        return this.clientConfig;
    }

    /**
     * This should return the payload without the function part of the payload.
     *
     * @return The payload after 'function'
     */
    protected abstract byte[] getPayload();

    protected abstract Command getCommand();

    public String getLogInfo(byte[] message) {
        List<String> hexParts = this.getHexParts(message);

        StringBuilder table = this.getHeader(hexParts);
        String hexLine = this.getMessageAsTableContent(hexParts, table);
        String seperatorLine = this.getTableSeperatorLine(table.length());
        table.append(System.lineSeparator()).append(seperatorLine);
        table.append(System.lineSeparator()).append(hexLine);
        table.append(System.lineSeparator()).append(seperatorLine);

        String line = null;
        if (LOG.isTraceEnabled()) {
            line = System.lineSeparator() + "Command: " + this.getCommand() + System.lineSeparator() +
                    "Payload: " + System.lineSeparator() + "\t" + Arrays.stream(this.getPayloadLogInfo()).collect(Collectors.joining(System.lineSeparator() + "\t")) + System.lineSeparator() +
                    "Length: " + message[1] + System.lineSeparator() +
                    "Checksum calculation steps: " + this.getMessageChecksumCalculationSteps(message) + " = " + message[message.length - 1] + System.lineSeparator() +
                    "Raw Bytes: " + Bytes.bytesToHex(message) + System.lineSeparator() +
                    seperatorLine + System.lineSeparator() +
                    table.toString();
        } else {
            line = "Command: " + this.getCommand() + ", " +
                    "Payload: " + String.join(", ", this.getPayloadLogInfo());
        }
        return line;
    }

    protected abstract String[] getPayloadLogInfo();

    private String getMessageChecksumCalculationSteps(byte[] message) {
        StringBuilder builder = new StringBuilder(100);
        for (int i = 0; i < message.length - 1; i++) {
            byte b = message[i];
            if (i > 0) {
                builder.append(" + ");
            }
            builder.append(b);
        }
        return builder.toString();
    }

    private static String getTableSeperatorLine(int size) {
        return IntStream.range(0, size).mapToObj(i -> "-").collect(Collectors.joining(""));
    }

    private String getMessageAsTableContent(Collection<String> parts, CharSequence builder) {
        String content = builder.toString();
        content = REMOVE_NAMES.matcher(content).replaceAll(" ");
        content = INSERT_PLACEHOLDERS.matcher(content).replaceAll("| %s");
        content = String.format(content, parts.toArray());
        return content;
    }

    private StringBuilder getHeader(Collection<String> parts) {
        StringBuilder builder = new StringBuilder(500);
        builder.append("| STX | Length | Command | ");
        for (int i = 0; i < parts.size() - 4; i++) {
            String logHeaderName = this.getLogHeaderName(i);
            if (logHeaderName == null) {
                logHeaderName = "Param " + i;
            }
            builder.append(logHeaderName).append(" | ");
        }
        builder.append("ChkSm |");
        return builder;
    }

    private List<String> getHexParts(byte[] message) {
        return Bytes.bytesToHexList(message);
    }

    protected String getLogHeaderName(int index) {
        return this.getMessageHandler().getCommandConfig(this.getCommand()).getParamNames().get(index);
    }

    protected String formatState(byte[] stateBytes, ComponentState... states) {
        return Arrays.stream(states).map(state -> "State: " + (state == null ? null : Bytes.bytesToHex(stateBytes))+ "\n" + Optional.ofNullable(state).map(ComponentState::prettyString).orElse(null)).collect(Collectors.joining(", "));
    }

    protected MessageHandler getMessageHandler() {
        return MessageHandlerFactory.getMessageHandler(this.getClientConfig().getCentralUnitType());
    }

    protected String formatFunction(Function function) {
        return "Function: " + function + " | " + this.getMessageHandler().getFunctionConfig(function).getNumber() + " | " + Bytes.bytesToHex((byte) this.getMessageHandler().getFunctionConfig(function).getNumber());
    }

    protected String formatOutput(int... numbers) {
        return Arrays.stream(numbers).mapToObj(number -> "Output: " + number + " | " + Bytes.bytesToHex(this.getMessageHandler().composeOutput(number))).collect(Collectors.joining(", "));
    }

    /**
     * Method for toString readability
     *
     * @return The Classname of the current class
     */
    @SuppressWarnings("UnusedDeclaration")
    public String getMessageClass() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }

    public List<EventMessage> respond(CentralUnit config, MessageHandler messageHandler) {
        return null;
    }

    public boolean isAcknowledged() {
        return this.acknowledged;
    }

    public void acknowledge() {
        this.acknowledged = true;
    }

    protected abstract String getId();
}
