package io.github.ridiekel.jeletask.end2end;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.ridiekel.jeletask.MockingTeletaskTestSupport;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessor;
import io.github.ridiekel.jeletask.utilities.StringUtilities;
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

        republishConfig();

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
            republishConfig();
        }
    }

    @Test
    void staleConfigOfThisBridgeIsRemoved() throws MqttException {
        MqttProcessor processor = this.mqtt().processor();
        String ownConfigs = "homeassistant/+/" + processor.getTeletaskIdentifier() + "/+/config";
        String stale = "homeassistant/switch/" + processor.getTeletaskIdentifier() + "/relay_999/config";
        String otherBridge = "homeassistant/switch/other_bridge/relay_999/config";
        Set<String> current = retained(ownConfigs).keySet();

        publishRetained(stale, "{}");
        publishRetained(otherBridge, "{}");
        try {
            processor.removeStaleConfig();

            assertThat(retained(ownConfigs).keySet()).isEqualTo(current);
            assertThat(retained(otherBridge)).containsOnlyKeys(otherBridge);
        } finally {
            clear(stale);
            clear(otherBridge);
        }
    }

    @Test
    void staleConfigFromBeforeStartupIsRemovedOnStartup() throws MqttException {
        assertThat(retained(this.mqtt().staleConfigTopicBeforeStartup())).isEmpty();
    }

    @Test
    void configUnderAPreviousTypeIsRemoved() throws MqttException {
        MqttProcessor processor = this.mqtt().processor();
        String relayConfigs = "homeassistant/+/" + processor.getTeletaskIdentifier() + "/relay_10/config";
        String current = onlyTopic(relayConfigs);
        String previousType = current.startsWith("homeassistant/light/") ? "switch" : "light";
        String previous = "homeassistant/" + previousType + "/" + processor.getTeletaskIdentifier() + "/relay_10/config";

        publishRetained(previous, "{}");
        try {
            processor.removeStaleConfig();

            assertThat(retained(relayConfigs)).containsOnlyKeys(current);
        } finally {
            clear(previous);
        }
    }

    @Test
    void configsThatAreNotOfThisBridgeAreLeftAlone() throws MqttException {
        MqttProcessor processor = this.mqtt().processor();
        List<String> notOfThisBridge = List.of(
                // Another bridge whose id starts with ours
                "homeassistant/switch/" + processor.getTeletaskIdentifier() + "_2/relay_999/config",
                // One level deeper than the config topics of the bridge
                "homeassistant/switch/" + processor.getTeletaskIdentifier() + "/relay_999/extra/config",
                // Without a node id
                "homeassistant/switch/relay_999/config"
        );

        for (String topic : notOfThisBridge) {
            publishRetained(topic, "{}");
        }
        try {
            processor.removeStaleConfig();

            for (String topic : notOfThisBridge) {
                assertThat(retained(topic)).containsOnlyKeys(topic);
            }
        } finally {
            for (String topic : notOfThisBridge) {
                clear(topic);
            }
        }
    }

    @Test
    void configUnderAPreviousDiscoveryPrefixIsRemoved() throws MqttException {
        MqttProcessor processor = this.mqtt().processor();
        String previousPrefixConfigs = "homeassistant/+/" + processor.getTeletaskIdentifier() + "/+/config";
        String stale = "custom/switch/" + processor.getTeletaskIdentifier() + "/relay_999/config";
        String originalPrefix = this.config.getMqtt().getDiscoveryPrefix();
        assertThat(retained(previousPrefixConfigs)).isNotEmpty();

        this.config.getMqtt().setDiscoveryPrefix("custom");
        publishRetained(stale, "{}");
        try {
            processor.removeStaleConfig();

            assertThat(retained(previousPrefixConfigs)).isEmpty();
            assertThat(retained(stale)).isEmpty();
        } finally {
            this.config.getMqtt().setDiscoveryPrefix(originalPrefix);
            clear(stale);
            // Back to the original prefix, and its config back for the other tests
            processor.removeStaleConfig();
            republishConfig();
        }
    }

    @Test
    void configOfAPreviousCentralIdIsRemoved() throws MqttException {
        String previous = "homeassistant/switch/previous_central_id/relay_10/config";

        publishRetained(previous, "{}");
        addToDiscoveryTopics(this.mqtt().processor().getDiscoveryTopicsTopic(), previous);
        try {
            this.mqtt().processor().removeStaleConfig();

            assertThat(retained(previous)).isEmpty();
        } finally {
            clear(previous);
        }
    }

    @Test
    void discoveryTopicsListTheCurrentConfig() throws MqttException {
        MqttProcessor processor = this.mqtt().processor();

        processor.removeStaleConfig();

        assertThat(discoveryTopics(processor.getDiscoveryTopicsTopic()))
                .containsExactlyInAnyOrderElementsOf(retained("homeassistant/+/" + processor.getTeletaskIdentifier() + "/+/config").keySet());
    }

    @Test
    void onlyConfigTopicsFromTheDiscoveryTopicsAreCleared() throws MqttException {
        String notAConfig = "somewhere/else/state";

        publishRetained(notAConfig, "ON");
        addToDiscoveryTopics(this.mqtt().processor().getDiscoveryTopicsTopic(), notAConfig);
        try {
            this.mqtt().processor().removeStaleConfig();

            assertThat(retained(notAConfig)).containsOnlyKeys(notAConfig);
        } finally {
            clear(notAConfig);
        }
    }

    @Test
    void discoveryTopicsOfAnotherBridgeAreIgnored() throws MqttException {
        String otherBridgeTopics = "teletask2mqtt/bridges/other_client_id/discovery";
        String otherBridgeConfig = "homeassistant/switch/other_central_id/relay_10/config";

        publishRetained(otherBridgeConfig, "{}");
        addToDiscoveryTopics(otherBridgeTopics, otherBridgeConfig);
        try {
            this.mqtt().processor().removeStaleConfig();

            assertThat(retained(otherBridgeConfig)).containsOnlyKeys(otherBridgeConfig);
        } finally {
            clear(otherBridgeConfig);
            clear(otherBridgeTopics);
        }
    }

    @Test
    void homeAssistantRemovesTheStaleEntity() throws MqttException {
        MqttProcessor processor = this.mqtt().processor();
        String relaySwitch = "homeassistant/switch/" + processor.getTeletaskIdentifier() + "/relay_11/config";
        String stale = "homeassistant/switch/" + processor.getTeletaskIdentifier() + "/relay_999/config";
        // A copy of an existing relay under a number that is not configured, so Home Assistant accepts it
        String staleConfig = new String(retained(relaySwitch).get(relaySwitch).getPayload())
                .replace("relay_11", "relay_999")
                .replace("relay-11", "relay-999");

        publishRetained(stale, staleConfig);
        try {
            awaitEntity("relay_999", true);

            processor.removeStaleConfig();

            awaitEntity("relay_999", false);
        } finally {
            clear(stale);
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

    /**
     * Like the bridge does when Home Assistant comes online, so the entities get their state back for the other tests.
     */
    private void republishConfig() {
        MqttProcessor processor = this.mqtt().processor();
        processor.publishConfig();
        processor.refreshStates();
        processor.publishConnectionStatus();
    }

    private String onlyTopic(String topicFilter) throws MqttException {
        Map<String, MqttMessage> found = retained(topicFilter);
        assertThat(found).hasSize(1);
        return found.keySet().iterator().next();
    }

    private void awaitEntity(String objectId, boolean present) {
        Awaitility.await("entity " + objectId + (present ? " present" : " removed"))
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> this.ha().teletaskStates().stream().anyMatch(e -> e.getEntity_id().endsWith("_" + objectId)) == present);
    }

    private List<String> discoveryTopics(String topic) throws MqttException {
        return Optional.ofNullable(retained(topic).get(topic))
                .map(message -> {
                    try {
                        return StringUtilities.OBJECT_MAPPER.readValue(message.getPayload(), new TypeReference<List<String>>() {
                        });
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .orElse(List.of());
    }

    private void addToDiscoveryTopics(String topic, String configTopic) throws MqttException {
        List<String> topics = new ArrayList<>(discoveryTopics(topic));
        topics.add(configTopic);
        try {
            publishRetained(topic, StringUtilities.OBJECT_MAPPER.writeValueAsString(topics));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void clear(String topic) throws MqttException {
        publishRetained(topic, "");
    }

    private void publishRetained(String topic, String payload) throws MqttException {
        MqttClient client = newClient();
        try {
            client.publish(topic, payload.getBytes(), 1, true);
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
