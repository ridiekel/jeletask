package io.github.ridiekel.jeletask.mockserver;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.spec.Command;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Picks the display messages out of what the bridge sends, as they pass on their way to {@link TeletaskMockServer}.
 * The message handler cannot parse a display message (the bridge only ever sends one), so the mock server would neither
 * see nor acknowledge it. A real central unit acknowledges it, so this does, and it keeps the frame for a test to check.
 */
final class DisplayMessageTap extends FilterInputStream {
    private static final int STX_INDEX = 0;
    private static final int LENGTH_INDEX = 1;
    private static final int COMMAND_INDEX = 2;

    private final MessageHandler messageHandler;
    private final OutputStream toTheBridge;
    private final ByteArrayOutputStream pending = new ByteArrayOutputStream();
    private final List<byte[]> displayMessages = new CopyOnWriteArrayList<>();

    DisplayMessageTap(InputStream fromTheBridge, OutputStream toTheBridge, MessageHandler messageHandler) {
        super(fromTheBridge);
        this.toTheBridge = toTheBridge;
        this.messageHandler = messageHandler;
    }

    List<byte[]> displayMessages() {
        return this.displayMessages;
    }

    void clear() {
        this.displayMessages.clear();
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b >= 0) {
            tap(new byte[]{(byte) b}, 0, 1);
        }
        return b;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int read = super.read(buffer, offset, length);
        if (read > 0) {
            tap(buffer, offset, read);
        }
        return read;
    }

    private synchronized void tap(byte[] buffer, int offset, int length) {
        this.pending.write(buffer, offset, length);
        byte[] bytes = this.pending.toByteArray();
        int start = 0;
        while (start < bytes.length) {
            if (bytes[start + STX_INDEX] != (byte) this.messageHandler.getStxValue()) {
                start++;
            } else if (start + LENGTH_INDEX >= bytes.length || start + bytes[start + LENGTH_INDEX] + 1 > bytes.length) {
                break;
            } else {
                // The length counts everything but the checksum
                byte[] frame = Arrays.copyOfRange(bytes, start, start + bytes[start + LENGTH_INDEX] + 1);
                start += frame.length;
                if (frame.length > COMMAND_INDEX && this.messageHandler.getCommand(frame[COMMAND_INDEX]) == Command.DISPLAYMESSAGE) {
                    this.displayMessages.add(frame);
                    acknowledge();
                }
            }
        }
        this.pending.reset();
        this.pending.write(bytes, start, bytes.length - start);
    }

    private void acknowledge() {
        try {
            this.toTheBridge.write(this.messageHandler.getAcknowledgeValue());
            this.toTheBridge.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
