package io.github.ridiekel.jeletask.mqtt.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.ridiekel.jeletask.HealthCheck;
import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessorFixtures.FakeTeletaskClient;
import io.github.ridiekel.jeletask.utilities.StringUtilities;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.github.ridiekel.jeletask.mqtt.listener.MqttProcessorFixtures.BASE_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MqttProcessorTest {
    private static final String AVAILABILITY_TOPIC = BASE_TOPIC + "/state";
    private static final String STOP_TOPIC = BASE_TOPIC + "/stop";
    private static final String REFRESH_TOPIC = BASE_TOPIC + "/refresh";
    private static final String DISCOVERY_TOPICS_TOPIC = "teletask2mqtt/bridges/fixture_client/discovery";
    private static final Duration AWAIT = Duration.ofSeconds(10);

    private final FakeTeletaskClient teletask = new FakeTeletaskClient();
    private final Teletask2MqttConfigurationProperties configuration = MqttProcessorFixtures.configuration();
    private final List<Integer> exitCodes = new CopyOnWriteArrayList<>();
    private FakeMqttClient mqtt;
    private final MqttProcessor processor = new MqttProcessor(
            MqttProcessorFixtures.centralUnit(),
            teletask,
            configuration,
            exitCodes::add,
            (_, _) -> mqtt = new FakeMqttClient()
    );

    private final Map<String, String> retainedOnBroker = new ConcurrentHashMap<>();

    /**
     * The connected status is static in MqttProcessor. Hand it back in its initial state, or a later bridge in this
     * JVM (an End2End test) sees no change and never publishes that it is online.
     */
    @AfterEach
    void resetStaticConnectedStatus() {
        teletask.connected = false;
        processor.checkAndPublishConnectedStatus();
    }

    @Test
    void connectionLostExitsWithStatus5WhenConfigured() {
        configuration.getMqtt().setExitOnDisconnect(true);
        processor.startup();

        mqtt.callback.connectionLost(new IOException("broker gone"));

        assertThat(exitCodes).containsExactly(5);
    }

    @Test
    void connectionLostKeepsRunningByDefault() {
        processor.startup();

        mqtt.callback.connectionLost(new IOException("broker gone"));

        assertThat(exitCodes).isEmpty();
    }

    @Test
    void reconnectSubscribesToEverythingAgainAndThenPublishesTheStatus() {
        processor.startup();
        processor.checkAndPublishConnectedStatus();
        mqtt.clearRecordings();

        mqtt.callback.connectComplete(true, "tcp://broker");

        awaitAvailabilityPublished("online");
        assertThat(mqtt.subscribed).containsExactlyInAnyOrder(
                BASE_TOPIC + "/+/+/set",
                BASE_TOPIC + "/ping",
                STOP_TOPIC,
                REFRESH_TOPIC,
                "homeassistant/status"
        );
    }

    @Test
    void reconnectStillPublishesTheStatusWhenSubscribingFails() {
        processor.startup();
        processor.checkAndPublishConnectedStatus();
        mqtt.clearRecordings();
        mqtt.failSubscribeTo = BASE_TOPIC + "/+/+/set";

        mqtt.callback.connectComplete(true, "tcp://broker");

        awaitAvailabilityPublished("online");
        // The shared executor survived the failure
        awaitNextGroupGetAfter(() -> mqtt.deliver(REFRESH_TOPIC, "still alive"));
    }

    @Test
    void firstConnectDoesNotSubscribeASecondTime() {
        processor.startup();
        assertThat(mqtt.subscribed).containsOnlyOnce(STOP_TOPIC);
        mqtt.clearRecordings();

        mqtt.callback.connectComplete(false, "tcp://broker");

        // Runs on the same single threaded executor as the reconnect work, so that would be done by then
        awaitNextGroupGetAfter(() -> mqtt.deliver(REFRESH_TOPIC, "sentinel"));
        assertThat(mqtt.subscribed).isEmpty();
    }

    @Test
    void stopTopicExitsWithStatus0() throws Exception {
        processor.startup();

        mqtt.deliver(STOP_TOPIC, "maintenance");

        assertThat(exitCodes).containsExactly(0);
    }

    @Test
    void refreshTopicRefreshesTheStatesWithoutExiting() {
        processor.startup();

        awaitNextGroupGetAfter(() -> mqtt.deliver(REFRESH_TOPIC, "because"));

        assertThat(exitCodes).isEmpty();
    }

    @Test
    void statesAreRefreshedPeriodically() {
        configuration.getPublish().setStatesInterval(1);
        processor.startup();

        Awaitility.await("periodic group gets").atMost(AWAIT).until(() -> teletask.groupGets.get() >= 3);
    }

    @Test
    void noPeriodicRefreshWhenTheIntervalIsZero() {
        processor.startup();
        assertThat(teletask.groupGets).hasValue(1);

        // The shortest possible interval is one second
        Awaitility.await("no periodic group get")
                .during(Duration.ofMillis(1500))
                .atMost(Duration.ofSeconds(3))
                .until(() -> teletask.groupGets.get() == 1);
    }

    @Test
    void pingThatIsNeverAnsweredMarksMqttDownUntilAPingSucceedsAgain() {
        processor.startup();
        awaitHealth("mqtt=UP");

        mqtt.echoPublishes = false;
        assertDoesNotThrow(processor::pingMqtt);
        assertThat(mqtt.isConnected()).isTrue();
        awaitHealth("mqtt=DOWN");

        mqtt.echoPublishes = true;
        processor.pingMqtt();
        awaitHealth("mqtt=UP");
    }

    @Test
    void pingIsNotRetained() {
        processor.startup();
        mqtt.clearRecordings();

        processor.pingMqtt();

        assertThat(mqtt.published).singleElement().satisfies(ping -> {
            assertThat(ping.topic()).isEqualTo(BASE_TOPIC + "/ping");
            assertThat(ping.retained()).isFalse();
        });
    }

    @Test
    void mqttIsDownInTheHealthStatusWhenTheClientDisconnected() {
        processor.startup();
        awaitHealth("mqtt=UP");

        mqtt.connected = false;

        awaitHealth("mqtt=DOWN");
    }

    @Test
    void mqttIsDownInTheHealthStatusBeforeTheClientExists() {
        awaitHealth("mqtt=DOWN");
    }

    @Test
    void teletaskStatusEndsUpInTheHealthStatus() {
        processor.startup();
        awaitHealth("teletask=UP");

        teletask.connected = false;

        awaitHealth("teletask=DOWN");
    }

    @Test
    void healthFileThatCannotBeWrittenIsNotFatal() throws IOException {
        // HealthCheck writes to this sibling first, and cannot when it is a directory
        Path blocked = Path.of(System.getProperty("java.io.tmpdir"), "jeletask2mqtt.health.tmp");
        Files.deleteIfExists(blocked);
        Files.createDirectory(blocked);
        try {
            assertThrows(IOException.class, () -> HealthCheck.getDefault().write(true, true));
            assertDoesNotThrow(processor::checkAndPublishConnectedStatus);
        } finally {
            Files.delete(blocked);
        }
    }

    @Test
    void failedPublishIsNotFatalAndLaterPublishesStillGoOut() {
        processor.startup();
        mqtt.clearRecordings();
        mqtt.failPublishTo = BASE_TOPIC + "/relay/1/state";
        ComponentSpec failing = MqttProcessorFixtures.relay(1);
        ComponentSpec next = MqttProcessorFixtures.relay(2);

        processor.receive(List.of(failing, next));

        assertThat(mqtt.publishAttempts).containsExactly(BASE_TOPIC + "/relay/1/state", BASE_TOPIC + "/relay/2/state");
        assertThat(mqtt.published).extracting(Published::topic).containsExactly(BASE_TOPIC + "/relay/2/state");
    }

    @Test
    void disconnectedClientIsNotPublishedTo() {
        processor.startup();
        mqtt.clearRecordings();
        mqtt.connected = false;
        ComponentSpec relay = MqttProcessorFixtures.relay(1);

        processor.publishState(relay, relay.getState());

        assertThat(mqtt.publishAttempts).isEmpty();
    }

    @Test
    void publishingBeforeTheClientExistsIsNotFatal() {
        ComponentSpec relay = MqttProcessorFixtures.relay(1);

        assertDoesNotThrow(() -> processor.publishState(relay, relay.getState()));
    }

    @Test
    void unreadableDiscoveryTopicsListIsIgnoredAndStartupContinues() throws IOException {
        String staleFromRetainedConfig = "homeassistant/switch/fixture_central/relay_998/config";
        retainedOnBroker.put(DISCOVERY_TOPICS_TOPIC, "this is not json");
        retainedOnBroker.put(staleFromRetainedConfig, "{}");

        processor.startup();

        assertThat(clearedTopics()).containsExactly(staleFromRetainedConfig);
        assertThat(publishedDiscoveryTopics()).hasSize(2).doesNotContain(staleFromRetainedConfig);
        assertThat(mqtt.subscribed).contains(STOP_TOPIC);
    }

    @Test
    void readableDiscoveryTopicsListClearsOnlyStaleConfigTopics() throws IOException {
        String staleOtherCentral = "homeassistant/switch/old_central/relay_5/config";
        String notAConfigTopic = "homeassistant/switch/old_central/relay_5/state";
        String alreadyCleared = "homeassistant/switch/fixture_central/relay_997/config";
        processorPublishedBefore(List.of(staleOtherCentral, notAConfigTopic));
        retainedOnBroker.put(alreadyCleared, "");

        processor.startup();

        assertThat(clearedTopics()).containsExactly(staleOtherCentral);
        assertThat(publishedDiscoveryTopics()).hasSize(2).doesNotContain(staleOtherCentral);
    }

    private void processorPublishedBefore(List<String> topics) throws IOException {
        retainedOnBroker.put(DISCOVERY_TOPICS_TOPIC, StringUtilities.OBJECT_MAPPER.writeValueAsString(topics));
    }

    private List<String> clearedTopics() {
        return mqtt.published.stream().filter(p -> p.retained() && p.payload().isEmpty()).map(Published::topic).toList();
    }

    private List<String> publishedDiscoveryTopics() throws IOException {
        String payload = mqtt.published.stream().filter(p -> p.topic().equals(DISCOVERY_TOPICS_TOPIC)).reduce((_, last) -> last).orElseThrow().payload();
        return StringUtilities.OBJECT_MAPPER.readValue(payload, new TypeReference<>() {
        });
    }

    private void awaitAvailabilityPublished(String expected) {
        Awaitility.await("availability " + expected).atMost(AWAIT).untilAsserted(() ->
                assertThat(mqtt.published).contains(new Published(AVAILABILITY_TOPIC, expected, true)));
    }

    private void awaitNextGroupGetAfter(ThrowingRunnable action) {
        int before = teletask.groupGets.get();
        assertDoesNotThrow(action::run);
        Awaitility.await("group get").atMost(AWAIT).until(() -> teletask.groupGets.get() > before);
    }

    /**
     * Retries because a Spring context cached by an End2End test can write the same file on its schedule in this JVM.
     */
    private void awaitHealth(String expected) {
        Awaitility.await("health " + expected).atMost(AWAIT).untilAsserted(() -> assertThat(health()).contains(expected));
    }

    private String health() {
        processor.checkAndPublishConnectedStatus();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HealthCheck.getDefault().check(new PrintStream(out));
        return out.toString();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private record Published(String topic, String payload, boolean retained) {
    }

    /**
     * Stands in for Paho and a broker: records what the bridge does, and hands published messages to matching
     * subscribers on the calling thread.
     */
    private final class FakeMqttClient extends MqttClient {
        final List<String> subscribed = new CopyOnWriteArrayList<>();
        final List<String> publishAttempts = new CopyOnWriteArrayList<>();
        final List<Published> published = new CopyOnWriteArrayList<>();
        private final Map<String, IMqttMessageListener> listeners = new ConcurrentHashMap<>();
        volatile MqttCallbackExtended callback;
        volatile boolean connected;
        volatile boolean echoPublishes = true;
        volatile String failSubscribeTo;
        volatile String failPublishTo;

        FakeMqttClient() throws MqttException {
            super("tcp://localhost:1", "fake", new MemoryPersistence());
        }

        void clearRecordings() {
            subscribed.clear();
            publishAttempts.clear();
            published.clear();
        }

        @Override
        public void setCallback(MqttCallback callback) {
            this.callback = (MqttCallbackExtended) callback;
        }

        @Override
        public void connect(MqttConnectOptions options) {
            connected = true;
            callback.connectComplete(false, getServerURI());
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public void subscribe(String topicFilter, int qos, IMqttMessageListener listener) throws MqttException {
            if (topicFilter.equals(failSubscribeTo)) {
                throw new MqttException(MqttException.REASON_CODE_SUBSCRIBE_FAILED);
            }
            subscribed.add(topicFilter);
            listeners.put(topicFilter, listener);
            for (Map.Entry<String, String> retained : retainedOnBroker.entrySet()) {
                if (MqttTopic.isMatched(topicFilter, retained.getKey())) {
                    MqttMessage message = new MqttMessage(retained.getValue().getBytes());
                    message.setRetained(true);
                    try {
                        listener.messageArrived(retained.getKey(), message);
                    } catch (Exception e) {
                        throw new IllegalStateException("Paho would drop the connection on this", e);
                    }
                }
            }
        }

        @Override
        public void unsubscribe(String[] topicFilters) {
            for (String topicFilter : topicFilters) {
                listeners.remove(topicFilter);
            }
        }

        @Override
        public void publish(String topic, MqttMessage message) throws MqttException {
            publishAttempts.add(topic);
            if (topic.equals(failPublishTo)) {
                throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
            }
            published.add(new Published(topic, new String(message.getPayload()), message.isRetained()));
            if (echoPublishes) {
                try {
                    deliver(topic, message);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        void deliver(String topic, String payload) throws Exception {
            deliver(topic, new MqttMessage(payload.getBytes()));
        }

        private void deliver(String topic, MqttMessage message) throws Exception {
            for (Map.Entry<String, IMqttMessageListener> listener : listeners.entrySet()) {
                if (MqttTopic.isMatched(listener.getKey(), topic)) {
                    listener.getValue().messageArrived(topic, message);
                }
            }
        }
    }
}
