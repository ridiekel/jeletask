package io.github.ridiekel.jeletask.mqtt;

import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.mqtt.container.ha.Entity;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("resource")
class TeletaskServiceTest extends TeletaskTestSupport {
    @Test
    void relayStateChange() {
//        System.out.println(ha().state("light.teletask_man_test_localhost_1234_relay_1"));

        set(Function.RELAY, 1, new ComponentState("OFF"));

        String broker = "tcp://localhost:" + mqtt().getPort();

        try {
            MqttClient mqttClient = new MqttClient(broker, UUID.randomUUID().toString(), new MemoryPersistence());
            mqttClient.connect();
            mqttClient.subscribe("+", (s, m) -> System.out.println(s + " - " + new String(m.getPayload())));
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }


        Awaitility.await().atMost(10, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
            Entity entity = ha().state("light.teletask_man_test_localhost_1234_relay_1");
//            System.out.println(entity);
            return !Objects.equals(entity.getState(), "unknown");
        });

//        this.ha().openBrowser();
    }
}

