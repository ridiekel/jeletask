package io.github.ridiekel.jeletask.mqtt.listener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Just enough of an MQTT 3.1.1 broker to accept one client, and to stop answering its subscribes when a test asks so:
 * a real broker cannot be made to keep the connection up and stay silent.
 */
final class FakeBroker implements AutoCloseable {
    private static final int CONNECT = 0x10;
    private static final int SUBSCRIBE = 0x80;
    private static final int PINGREQ = 0xC0;

    private final ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    private final AtomicInteger subscribesSeen = new AtomicInteger();
    private volatile boolean answerSubscribes = true;
    private volatile OutputStream toClient;

    FakeBroker() throws IOException {
        Thread.ofVirtual().start(this::serve);
    }

    int port() {
        return this.server.getLocalPort();
    }

    int subscribesSeen() {
        return this.subscribesSeen.get();
    }

    void stopAnsweringSubscribes() {
        this.answerSubscribes = false;
    }

    /**
     * A QoS 0 message, the way the broker hands one to a subscriber.
     */
    void publish(String topic, String payload) throws IOException {
        byte[] topicBytes = topic.getBytes(StandardCharsets.UTF_8);
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream packet = new ByteArrayOutputStream();
        packet.write(0x30);
        // Short enough for a remaining length of one byte
        packet.write(2 + topicBytes.length + payloadBytes.length);
        packet.write(topicBytes.length >> 8);
        packet.write(topicBytes.length);
        packet.writeBytes(topicBytes);
        packet.writeBytes(payloadBytes);
        this.send(packet.toByteArray());
    }

    private void serve() {
        try (Socket socket = this.server.accept()) {
            InputStream in = socket.getInputStream();
            this.toClient = socket.getOutputStream();
            while (true) {
                int header = in.read();
                if (header < 0) {
                    return;
                }
                byte[] body = in.readNBytes(remainingLength(in));
                this.answer(header & 0xF0, body);
            }
        } catch (IOException _) {
            // The client or the test closed the connection: that ends this exchange.
        }
    }

    private void answer(int type, byte[] body) throws IOException {
        if (type == CONNECT) {
            // CONNACK, session not present, accepted
            this.send(new byte[]{0x20, 0x02, 0x00, 0x00});
        } else if (type == PINGREQ) {
            this.send(new byte[]{(byte) 0xD0, 0x00});
        } else if (type == SUBSCRIBE) {
            this.subscribesSeen.incrementAndGet();
            if (this.answerSubscribes) {
                // SUBACK for the packet id in the first two bytes, granted QoS 0
                this.send(new byte[]{(byte) 0x90, 0x03, body[0], body[1], 0x00});
            }
        }
    }

    private synchronized void send(byte[] packet) throws IOException {
        this.toClient.write(packet);
        this.toClient.flush();
    }

    private static int remainingLength(InputStream in) throws IOException {
        int value = 0;
        int multiplier = 1;
        int digit;
        do {
            digit = in.read();
            if (digit < 0) {
                throw new IOException("closed");
            }
            value += (digit & 0x7F) * multiplier;
            multiplier *= 128;
        } while ((digit & 0x80) != 0);
        return value;
    }

    @Override
    public void close() throws IOException {
        this.server.close();
    }
}
