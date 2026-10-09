package io.github.ridiekel.jeletask.end2end;

import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessor;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class End2EndRetainedTest extends MockingTeletaskTestSupport {
    @AfterEach
    void restoreDefaults() throws MqttException {
        this.config.getMqtt().setRetained(false);
        // States retained during a test would stick around on the broker for the other tests
        for (String topic : retained(this.config.getMqtt().getPrefix() + "/+/+/+/state").keySet()) {
            clear(topic);
        }
    }

    @ParameterizedTest(name = "retained flag {0}")
    @ValueSource(booleans = {false, true})
    void discoveryConfigIsAlwaysRetained(boolean retainedFlag) throws MqttException {
        MqttProcessor processor = this.mqtt().processor();
        // Start from an empty broker, otherwise the config from startup would make this pass anyway
        processor.removeConfig();
        this.config.getMqtt().setRetained(retainedFlag);

        processor.publishConfig();

        assertThat(retained("homeassistant/+/+/+/config")).isNotEmpty();
    }

    @ParameterizedTest(name = "retained flag {0}")
    @ValueSource(booleans = {false, true})
    void removingTheConfigClearsTheRetainedConfig(boolean retainedFlag) throws MqttException {
        MqttProcessor processor = this.mqtt().processor();
        this.config.getMqtt().setRetained(retainedFlag);
        try {
            processor.removeConfig();

            assertThat(retained("homeassistant/+/+/+/config")).isEmpty();
        } finally {
            processor.publishConfig();
        }
    }

    @Test
    void statesAreOnlyRetainedWhenConfigured() throws MqttException {
        String stateTopic = this.config.getMqtt().getPrefix() + "/+/relay/10/state";

        this.teletask().relay(10).turnOff();
        this.mqtt().expect().relay(10).lastStateMessage().toHave().state().off();
        this.teletask().relay(10).turnOn();
        this.mqtt().expect().relay(10).lastStateMessage().toHave().state().on();

        assertThat(retained(stateTopic)).isEmpty();

        this.config.getMqtt().setRetained(true);
        this.teletask().relay(10).turnOff();
        this.mqtt().expect().relay(10).lastStateMessage().toHave().state().off();

        assertThat(retained(stateTopic)).hasSize(1);
    }

    @ParameterizedTest(name = "retained flag {0}")
    @ValueSource(booleans = {false, true})
    void pingIsNeverRetained(boolean retainedFlag) throws MqttException {
        this.config.getMqtt().setRetained(retainedFlag);

        this.mqtt().processor().pingMqtt();

        assertThat(retained(this.config.getMqtt().getPrefix() + "/+/ping")).isEmpty();
    }

    /**
     * Subscribes with a fresh client, so only messages the broker kept can show up.
     */
    private Map<String, MqttMessage> retained(String topicFilter) throws MqttException {
        Map<String, MqttMessage> received = new ConcurrentHashMap<>();
        MqttClient client = newClient();
        try {
            client.subscribe(topicFilter, (topic, message) -> {
                // An empty retained message is the broker telling us there is nothing kept
                if (message.isRetained() && message.getPayload().length > 0) {
                    received.put(topic, message);
                }
            });
            // The broker delivers retained messages right after subscribing, give it a moment to send them all
            Awaitility.await("retained messages on " + topicFilter)
                    .pollDelay(2, TimeUnit.SECONDS)
                    .atMost(3, TimeUnit.SECONDS)
                    .until(() -> true);
        } finally {
            client.disconnect();
            client.close();
        }
        return received;
    }

    private void clear(String topic) throws MqttException {
        MqttClient client = newClient();
        try {
            client.publish(topic, new byte[0], 1, true);
        } finally {
            client.disconnect();
            client.close();
        }
    }

    private MqttClient newClient() throws MqttException {
        String broker = "tcp://" + this.mqtt().getHost() + ":" + this.mqtt().getPort();
        MqttClient client = new MqttClient(broker, "retained-test-" + UUID.randomUUID(), new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        client.connect(options);
        return client;
    }
}
