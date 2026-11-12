package io.github.ridiekel.jeletask.client.builder.message;

import io.github.ridiekel.jeletask.TeletaskReceiver;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class MessageUtilities {
    private static final long FRAGMENT_SILENCE_MILLIS = 500;
    private static final long MAX_RECEIVE_MILLIS = 5000;
    private static final int MIN_MESSAGE_LENGTH = 3; // STX, length and command

    private MessageUtilities() {
    }

    public static List<MessageSupport> receive(Logger logger, TeletaskReceiver teletaskReceiver) throws IOException, InterruptedException {
        return receive(logger, teletaskReceiver, null);
    }

    public static List<MessageSupport> receive(Logger logger, TeletaskReceiver teletaskReceiver, MessageSupport currentlyRunningMessage) throws IOException, InterruptedException {
        List<MessageSupport> responses = new ArrayList<>();

        InputStream inputStream = teletaskReceiver.getInputStream();

        if (inputStream == null) {
            return responses;
        }

        byte[] overflow = new byte[0];
        long startTime = System.currentTimeMillis();
        long lastDataTime = startTime;
        do {
            int available = inputStream.available();
            if (available > 0) {
                byte[] read = new byte[available];
                inputStream.read(read, 0, available);
                overflow = extractMessages(logger, teletaskReceiver.getCentralUnit(), responses, Bytes.concat(overflow, read), currentlyRunningMessage);
                lastDataTime = System.currentTimeMillis();
            }
            long now = System.currentTimeMillis();
            if (overflow.length > 0 && (now - lastDataTime > FRAGMENT_SILENCE_MILLIS || now - startTime > MAX_RECEIVE_MILLIS)) {
                overflow = dropIncompleteMessage(logger, teletaskReceiver.getCentralUnit(), responses, overflow, currentlyRunningMessage);
            }
            Thread.sleep(10);
        } while (overflow.length > 0);

        return responses;
    }

    /**
     * Only the message that never completed goes: a corrupt length makes it claim the messages behind it, those still count.
     */
    private static byte[] dropIncompleteMessage(Logger logger, CentralUnit centralUnit, Collection<MessageSupport> responses, byte[] overflow, MessageSupport currentlyRunningMessage) {
        int next = indexOfStx(centralUnit.getMessageHandler(), overflow, 1, overflow.length);
        byte[] dropped = Arrays.copyOfRange(overflow, 0, next);
        if (logger.isWarnEnabled()) {
            logger.warn("Receive - Dropping incomplete message: {}", Bytes.bytesToHex(dropped));
        }
        if (next == overflow.length) {
            return new byte[0];
        }
        return extractMessages(logger, centralUnit, responses, Arrays.copyOfRange(overflow, next, overflow.length), currentlyRunningMessage);
    }

    private static int indexOfStx(MessageHandler messageHandler, byte[] data, int from, int to) {
        for (int i = from; i < to; i++) {
            if (data[i] == messageHandler.getStxValue()) {
                return i;
            }
        }
        return to;
    }

    private static boolean hasCorrectChecksum(byte[] message) {
        byte sum = 0;
        for (int i = 0; i < message.length - 1; i++) {
            sum += message[i];
        }
        return sum == message[message.length - 1];
    }

    private static byte[] extractMessages(Logger logger, CentralUnit centralUnit, Collection<MessageSupport> responses, byte[] data, MessageSupport currentlyRunningMessage) {
        logger.trace("Receive - Raw bytes: {}", () -> Bytes.bytesToHex(data));
        MessageHandler messageHandler = centralUnit.getMessageHandler();
        int i = 0;
        while (i < data.length) {
            byte b = data[i];
            logger.trace("Receive - Processing byte: {}", () -> Bytes.bytesToHex(b));
            if (b != messageHandler.getStxValue()) {
                handleNonStxByte(logger, messageHandler, b, currentlyRunningMessage);
                i++;
            } else if (hasImpossibleLength(data, i)) {
                byte length = data[i + 1];
                logger.warn("Receive - Skipping STX followed by impossible length: {}", () -> Bytes.bytesToHex(length));
                i++;
            } else if (isIncomplete(data, i)) {
                byte[] overflow = Arrays.copyOfRange(data, i, data.length);
                logger.trace("Receive - Overflowing following byte[]: {}", () -> Bytes.bytesToHex(overflow));
                return overflow;
            } else {
                int eventLengthInclChkSum = data[i + 1] + 1; // +1 for checksum
                byte[] event = Arrays.copyOfRange(data, i, i + eventLengthInclChkSum);
                logger.trace("Receive - Found message bytes: {}", () -> Bytes.bytesToHex(event));
                if (hasCorrectChecksum(event)) {
                    parseMessage(logger, centralUnit, messageHandler, event, responses);
                    i += eventLengthInclChkSum;
                } else {
                    // A corrupt length may have taken the start of the next message along, so look for it inside
                    int next = indexOfStx(messageHandler, data, i + 1, i + eventLengthInclChkSum);
                    byte[] skipped = Arrays.copyOfRange(data, i, next);
                    logger.warn("Receive - Skipping bytes with a wrong checksum: {}", () -> Bytes.bytesToHex(skipped));
                    i = next;
                }
            }
        }
        return new byte[0];
    }

    private static boolean hasImpossibleLength(byte[] data, int stxIndex) {
        return stxIndex + 1 < data.length && data[stxIndex + 1] < MIN_MESSAGE_LENGTH;
    }

    private static boolean isIncomplete(byte[] data, int stxIndex) {
        return stxIndex + 1 == data.length || stxIndex + data[stxIndex + 1] + 1 > data.length;
    }

    private static void parseMessage(Logger logger, CentralUnit centralUnit, MessageHandler messageHandler, byte[] event, Collection<MessageSupport> responses) {
        try {
            MessageSupport parse = messageHandler.parse(centralUnit, event);
            if (parse != null) {
                responses.add(parse);
            }
        } catch (CentralUnit.ComponentNotFoundInConfigException e) {
            logger.debug("Exception ({}) caught in extractMessages: {}", e.getClass().getName(), e.getMessage());
        } catch (Exception e) {
            logger.error("Exception ({}) caught in extractMessages: {}", e.getClass().getName(), e.getMessage(), e);
        }
    }

    private static void handleNonStxByte(Logger logger, MessageHandler messageHandler, byte b, MessageSupport currentlyRunningMessage) {
        if (b == messageHandler.getAcknowledgeValue()) {
            logger.trace("Received acknowledge");
            if (currentlyRunningMessage != null) {
                currentlyRunningMessage.acknowledge();
            } else {
                logger.trace("Received an acknowledge, but there is no currently running message");
            }
        } else {
            logger.warn("Receive - Found byte, but don't know how to handle it: {}", () -> Bytes.bytesToHex(b));
        }
    }

    public static void send(OutputStream outputStream, byte[] message) throws IOException {
        outputStream.write(message);
        outputStream.flush();
    }
}
