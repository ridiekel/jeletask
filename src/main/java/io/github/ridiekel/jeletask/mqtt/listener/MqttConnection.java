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
     * again: the caller decides whether a broker that is not there yet is worth waiting for.
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
    record Message(byte[] payload, boolean retained) {
        public Message {
            payload = payload.clone();
        }

        public static Message of(String payload) {
            return new Message(payload.getBytes(StandardCharsets.UTF_8), false);
        }

        @Override
        public byte[] payload() {
            return this.payload.clone();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Message(byte[] otherPayload, boolean otherRetained) && this.retained == otherRetained && Arrays.equals(this.payload, otherPayload);
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
