package io.github.ridiekel.jeletask.mqtt.listener;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * The few things the bridge needs from an MQTT client, so the processor does not depend on one library.
 * <p>
 * Every blocking call gives up after a while: an answer of the broker that never comes must not hang the caller.
 */
public interface MqttConnection {
    /**
     * Connects and from then on reconnects by itself. Fails when the first connect does not succeed, without trying
     * again.
     */
    void connect(MqttConnectionSettings settings, Listener listener) throws MqttConnectionException;

    boolean isConnected();

    /**
     * Subscribing to a filter again replaces its handler, it never adds a second one.
     */
    void subscribe(String topicFilter, MessageHandler handler) throws MqttConnectionException;

    void unsubscribe(String... topicFilters) throws MqttConnectionException;

    /**
     * Sends with QoS 0.
     */
    void publish(String topic, byte[] payload, boolean retained) throws MqttConnectionException;

    void close();

    interface Listener {
        void connected(boolean reconnect);

        void connectionLost(Throwable cause);
    }

    @FunctionalInterface
    interface MessageHandler {
        void arrived(String topic, Message message);
    }

    @FunctionalInterface
    interface Factory {
        MqttConnection create();
    }

    /**
     * A message from the broker. {@link #toString()} is the payload as text.
     */
    final class Message {
        private final byte[] payload;
        private final boolean retained;

        public Message(byte[] payload, boolean retained) {
            this.payload = payload.clone();
            this.retained = retained;
        }

        public static Message of(String payload) {
            return new Message(payload.getBytes(StandardCharsets.UTF_8), false);
        }

        public byte[] getPayload() {
            return this.payload.clone();
        }

        public boolean isRetained() {
            return this.retained;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Message message && this.retained == message.retained && Arrays.equals(this.payload, message.payload);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(this.payload) + Boolean.hashCode(this.retained);
        }

        @Override
        public String toString() {
            return new String(this.payload, StandardCharsets.UTF_8);
        }
    }
}
