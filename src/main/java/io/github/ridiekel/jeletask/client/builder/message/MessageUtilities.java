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

    public static List<MessageSupport> receive(Logger logger, TeletaskReceiver teletaskReceiver) throws Exception {
        return receive(logger, teletaskReceiver, null);
    }

    public static List<MessageSupport> receive(Logger logger, TeletaskReceiver teletaskReceiver, MessageSupport currentlyRunningMessage) throws Exception {
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
                if (logger.isWarnEnabled()) {
                    logger.warn("Receive - Dropping incomplete message: {}", Bytes.bytesToHex(overflow));
                }
                overflow = new byte[0];
            }
            Thread.sleep(10);
        } while (overflow.length > 0);

        return responses;
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
                parseMessage(logger, centralUnit, messageHandler, event, responses);
                i += eventLengthInclChkSum;
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
