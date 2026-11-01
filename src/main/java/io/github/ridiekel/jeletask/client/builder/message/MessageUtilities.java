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
                logger.warn("Receive - Dropping incomplete message: {}", Bytes.bytesToHex(overflow));
                overflow = new byte[0];
            }
            Thread.sleep(10);
        } while (overflow.length > 0);

        return responses;
    }

    private static byte[] extractMessages(Logger logger, CentralUnit centralUnit, Collection<MessageSupport> responses, byte[] data, MessageSupport currentlyRunningMessage) throws Exception {
        logger.trace("Receive - Raw bytes: {}", Bytes.bytesToHex(data));
        MessageHandler messageHandler = centralUnit.getMessageHandler();
        byte[] overflow = new byte[0];
        for (int i = 0; i < data.length; i++) {
            byte b = data[i];
            logger.trace("Receive - Processing byte: {}", Bytes.bytesToHex(b));
            if (b == messageHandler.getStxValue()) {
                if (i + 1 < data.length && data[i + 1] < MIN_MESSAGE_LENGTH) {
                    logger.warn("Receive - Skipping STX followed by impossible length: {}", Bytes.bytesToHex(data[i + 1]));
                } else if (i + 1 == data.length || i + data[i + 1] + 1 > data.length) {
                    overflow = Arrays.copyOfRange(data, i, data.length);
                    i = data.length - 1;

                    logger.trace("Receive - Overflowing following byte[]: {}", Bytes.bytesToHex(overflow));
                } else {
                    int eventLengthInclChkSum = data[i + 1] + 1; // +1 for checksum
                    byte[] event = new byte[eventLengthInclChkSum];
                    System.arraycopy(data, i, event, 0, eventLengthInclChkSum);

                    i += eventLengthInclChkSum - 1;

                    logger.trace("Receive - Found message bytes: {}", Bytes.bytesToHex(event));
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
            } else if (b == messageHandler.getAcknowledgeValue()) {
                logger.trace("Received acknowledge");
                if (currentlyRunningMessage != null) {
                    currentlyRunningMessage.acknowledge();
                } else {
                    logger.trace("Received an acknowledge, but there is no currently running message");
                }
            } else {
                logger.warn("Receive - Found byte, but don't know how to handle it: {}", Bytes.bytesToHex(b));
            }
        }
        return overflow;
    }

    public static void send(OutputStream outputStream, byte[] message) throws IOException {
        outputStream.write(message);
        outputStream.flush();
    }
}
