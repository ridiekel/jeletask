package io.github.ridiekel.jeletask.mqtt.listener;

import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties.MqttConfiguration;

import java.time.Duration;

/**
 * How to reach the broker and what it has to know about us.
 *
 * @param username   null for an anonymous connection
 * @param password   null for none; only sent together with a username, MQTT 3.1.1 has no password on its own
 * @param willTopic  where the broker publishes {@code willPayload}, retained, once we disappear without disconnecting
 */
public record MqttConnectionSettings(
        MqttConfiguration mqtt,
        String clientId,
        String username,
        String password,
        Duration keepAlive,
        Duration connectTimeout,
        Duration reconnectDelay,
        Duration maxReconnectDelay,
        Duration timeToWait,
        String willTopic,
        String willPayload
) {
}
