package io.github.ridiekel.jeletask.end2end;

import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessor;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
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

        String broker = "tcp://" + this.mqtt().getHost() + ":" + this.mqtt().getPort();
        MqttClient bridge = new MqttClient(broker, "last-will-test-client", new MemoryPersistence());
        try {
            bridge.connect(processor.connectOptions(null, null));

            // Drop the connection without a DISCONNECT packet, like a crash would
            bridge.disconnectForcibly(0, 0, false);

            awaitLast(received, "offline");
        } finally {
            bridge.close(true);
            processor.publishConnectionStatus();
        }

        awaitLast(received, "online");
    }

    private static void awaitLast(List<String> received, String expected) {
        Awaitility.await("availability " + expected)
                .pollInterval(250, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> !received.isEmpty() && Objects.equals(received.getLast(), expected));
    }
}
