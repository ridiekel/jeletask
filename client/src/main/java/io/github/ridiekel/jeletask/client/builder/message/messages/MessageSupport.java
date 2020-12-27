package io.github.ridiekel.jeletask.client.builder.message.messages;

import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.client.builder.ByteUtilities;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandlerFactory;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.FunctionConfigurable;
import io.github.ridiekel.jeletask.client.builder.message.MessageUtilities;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.EventMessage;
import io.github.ridiekel.jeletask.model.spec.CentralUnit;
import io.github.ridiekel.jeletask.model.spec.Command;
import io.github.ridiekel.jeletask.model.spec.ComponentSpec;
import io.github.ridiekel.jeletask.model.spec.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
    public static final int ACK_WAIT_TIME = 1000;
    public static final int MAX_RETRY = 3;

    private final CentralUnit clientConfig;

    private boolean acknowledged = false;

    protected MessageSupport(CentralUnit clientConfig) {
        this.clientConfig = clientConfig;
    }

    public void execute(TeletaskClient client) {
        this.execute(client, 1);
    }

    private void execute(TeletaskClient client, int counter) {
        MessageHandler messageHandler = this.getMessageHandler();
        if (this.isValid()) {
            if (messageHandler.knows(this.getCommand())) {
                byte[] message = messageHandler.compose(this.getCommand(), this.getPayload());

                try {
                    client.send(message, this::getLogInfo);

                    this.waitForAcknowledge(client, counter);
                } catch (Exception e) {
                    LOG.error("Exception ({}) caught in execute: {}", e.getClass().getName(), e.getMessage(), e);
                }
            } else {
                LOG.warn("Message handler '{}' does not know of command '{}'", this.getMessageHandler().getClass().getSimpleName(), this.getCommand());
            }
        } else {
            LOG.warn("Invalid request: {}", this);
        }
    }

    private void waitForAcknowledge(TeletaskClient client, int counter) {
        long start = System.currentTimeMillis();
        while (!this.isAcknowledged() && (System.currentTimeMillis() - start) < ACK_WAIT_TIME) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                LOG.error("Exception ({}) caught in execute: {}", e.getClass().getName(), e.getMessage(), e);
            }
            try {
                client.handleReceiveEvents(MessageUtilities.receive(LOG, client, this));
            } catch (Exception e) {
                LOG.error("Exception ({}) caught in execute: {}", e.getClass().getName(), e.getMessage(), e);
            }
        }

        if (!this.isAcknowledged()) {
            LOG.warn("Did not receive an acknowledgement within " + ACK_WAIT_TIME + " milliseconds.");
            if (counter < MAX_RETRY) {
                counter++;
                LOG.warn("Retrying: attempt {} of {}", counter, MAX_RETRY);
                this.execute(client, counter);
            } else {
                LOG.error("We tried {} times to execute this message and never received an acknowledge from the central unit. Something is wrong", MAX_RETRY);
                throw new RuntimeException("Could not get the message to execute within the given amount of time.");
            }
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
                    "Raw Bytes: " + ByteUtilities.bytesToHex(message) + System.lineSeparator() +
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
        for (int i = 1; i <= parts.size() - 4; i++) {
            String logHeaderName = this.getLogHeaderName(i);
            if (logHeaderName == null) {
                logHeaderName = "Parameter " + i;
            }
            builder.append(logHeaderName).append(" | ");
        }
        builder.append("ChkSm |");
        return builder;
    }

    private List<String> getHexParts(byte[] message) {
        return ByteUtilities.bytesToHexList(message);
    }

    protected String getLogHeaderName(int index) {
        return this.getMessageHandler().getCommandConfig(this.getCommand()).getParamNames().get(index);
    }

    protected String formatState(Function function, int number, String... states) {
        FunctionConfigurable functionConfig = this.getMessageHandler().getFunctionConfig(function);
        ComponentSpec component = this.getClientConfig().getComponent(function, number);
        return Arrays.stream(states).map(state -> {
            byte[] bytes = functionConfig.getStateCalculator().convertSet(component, state);
            return "State: " + state + " | " + functionConfig.getStateCalculator().getNumberConverter().convert(bytes) + " | " + (state == null ? null : ByteUtilities.bytesToHex(bytes));
        }).collect(Collectors.joining(", "));
    }

    protected MessageHandler getMessageHandler() {
        return MessageHandlerFactory.getMessageHandler(this.getClientConfig().getCentralUnitType());
    }

    protected String formatFunction(Function function) {
        return "Function: " + function + " | " + this.getMessageHandler().getFunctionConfig(function).getNumber() + " | " + ByteUtilities.bytesToHex((byte) this.getMessageHandler().getFunctionConfig(function).getNumber());
    }

    protected String formatOutput(int... numbers) {
        return Arrays.stream(numbers).mapToObj(number -> "Output: " + number + " | " + ByteUtilities.bytesToHex(this.getMessageHandler().composeOutput(number))).collect(Collectors.joining(", "));
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
        return "MessageSupport{" + "clientConfig=" + this.clientConfig +
                ", acknowledged=" + this.acknowledged +
                '}';
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
}
