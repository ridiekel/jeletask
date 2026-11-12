package io.github.ridiekel.jeletask.end2end;

import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessor;
import org.awaitility.Awaitility;
import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties.MqttConfiguration;
import io.github.ridiekel.jeletask.mqtt.listener.HiveMqConnection;
import io.github.ridiekel.jeletask.mqtt.listener.MqttConnection;
import io.github.ridiekel.jeletask.mqtt.listener.MqttConnectionSettings;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

class End2EndLastWillTest extends MockingTeletaskTestSupport {
    @Test
    void brokerPublishesOfflineWhenBridgeDisappears() throws Exception {
        MqttProcessor processor = this.mqtt().processor();
        String topic = processor.getAvailabilityTopic();

        List<String> received = new CopyOnWriteArrayList<>();
        this.mqtt().subscribe(topic, (_, message) -> received.add(new String(message.getPayload())));

        // Retained, so a new subscriber gets it straight away
        awaitLast(received, "online");

        MqttConnection bridge = new HiveMqConnection();
        try (TcpRelay relay = new TcpRelay(this.mqtt().getHost(), this.mqtt().getPort())) {
            bridge.connect(throughRelay(processor.connectionSettings(null, null), relay), new MqttConnection.Listener() {
                @Override
                public void connected(boolean reconnect) {
                    // Only the last will matters here
                }

                @Override
                public void connectionLost(Throwable cause) {
                    // Expected: the relay drops the connection
                }
            });

            // Drop the connection without a DISCONNECT packet, like a crash would
            relay.cut();

            awaitLast(received, "offline");
        } finally {
            bridge.close();
            processor.publishConnectionStatus();
        }

        awaitLast(received, "online");
    }

    /**
     * The settings of the bridge, under another client id: the same one would push the running bridge off the broker.
     */
    private static MqttConnectionSettings throughRelay(MqttConnectionSettings bridge, TcpRelay relay) {
        MqttConfiguration mqtt = new MqttConfiguration();
        mqtt.setHost(relay.host());
        mqtt.setPort(String.valueOf(relay.port()));
        return new MqttConnectionSettings(mqtt, "last-will-test-client", bridge.username(), bridge.password(), bridge.keepAlive(), bridge.connectTimeout(),
                bridge.reconnectDelay(), bridge.maxReconnectDelay(), bridge.timeToWait(), bridge.willTopic(), bridge.willPayload());
    }

    private static void awaitLast(List<String> received, String expected) {
        Awaitility.await("availability " + expected)
                .pollInterval(250, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> !received.isEmpty() && Objects.equals(received.getLast(), expected));
    }
}
