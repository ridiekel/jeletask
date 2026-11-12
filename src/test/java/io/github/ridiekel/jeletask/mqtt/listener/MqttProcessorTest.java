package io.github.ridiekel.jeletask.mqtt.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.ridiekel.jeletask.HealthCheck;
import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessorFixtures.FakeTeletaskClient;
import io.github.ridiekel.jeletask.utilities.StringUtilities;
import org.awaitility.Awaitility;
// Only for its topic matching, the bridge itself no longer uses Paho
import org.eclipse.paho.client.mqttv3.MqttTopic;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.ridiekel.jeletask.mqtt.listener.MqttProcessorFixtures.BASE_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MqttProcessorTest {
    private static final String AVAILABILITY_TOPIC = BASE_TOPIC + "/state";
    private static final String STOP_TOPIC = BASE_TOPIC + "/stop";
    private static final String REFRESH_TOPIC = BASE_TOPIC + "/refresh";
    private static final String RELAY_1_SET_TOPIC = BASE_TOPIC + "/relay/1/set";
    private static final String DISCOVERY_TOPICS_TOPIC = "teletask2mqtt/bridges/fixture_client/discovery";
    private static final String RELAY_1_CONFIG_TOPIC = "homeassistant/light/fixture_central/relay_1/config";
    private static final String BRIDGE_CONFIG_TOPIC = "homeassistant/sensor/fixture_central/sensor_-1/config";
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
            () -> mqtt = new FakeMqttClient()
    );

    private final Map<String, String> retainedOnBroker = new ConcurrentHashMap<>();
    // Published by someone else while the bridge subscribes, so not retained
    private final Map<String, String> liveOnBroker = new ConcurrentHashMap<>();

    /**
     * Characterization, not a wish: no retry of its own. The application stops, and its restart policy brings it back.
     */
    @Test
    void startupFailsWhenTheBrokerCannotBeReached() {
        MqttConnectionException cause = new MqttConnectionException("Could not connect: Connection refused");
        MqttProcessor unreachable = new MqttProcessor(MqttProcessorFixtures.centralUnit(), teletask, configuration, exitCodes::add, () -> {
            FakeMqttClient client = new FakeMqttClient();
            client.failConnect = cause;
            return client;
        });

        IllegalStateException failure = assertThrows(IllegalStateException.class, unreachable::startup);

        assertThat(failure).hasCause(cause);
        assertThat(teletask.groupGets).hasValue(0);
    }

    @Test
    void blankCentralIdAndPrefixFallBackToTheCentralUnitAddressAndTheClientId() {
        configuration.getCentral().setId("  ");
        configuration.getMqtt().setPrefix(" ");
        CentralUnit centralUnit = MqttProcessorFixtures.centralUnit();
        centralUnit.setHost("192.168.0.5");
        centralUnit.setPort(55000);

        MqttProcessor fallback = new MqttProcessor(centralUnit, teletask, configuration, exitCodes::add, FakeMqttClient::new);

        assertThat(fallback.getTeletaskIdentifier()).isEqualTo("192_168_0_5_55000");
        assertThat(fallback.getPrefix()).isEqualTo("fixture_client");
        assertThat(fallback.getAvailabilityTopic()).isEqualTo("fixture_client/192_168_0_5_55000/state");
    }

    @Test
    void aPasswordWithoutAUsernameIsNotSent() {
        configuration.getMqtt().setUsername("  ");
        configuration.getMqtt().setPassword("secret");

        processor.startup();

        // MQTT 3.1.1 only allows a password together with a username
        assertThat(mqtt.settings.username()).isNull();
        assertThat(mqtt.settings.password()).isNull();
    }

    @Test
    void usernameAndPasswordAreSentTogether() {
        configuration.getMqtt().setUsername("bridge");
        configuration.getMqtt().setPassword("secret");

        processor.startup();

        assertThat(mqtt.settings.username()).isEqualTo("bridge");
        assertThat(mqtt.settings.password()).isEqualTo("secret");
    }

    @Test
    void usernameIsTrimmedAndNoPasswordIsSentWhenNoneIsConfigured() {
        configuration.getMqtt().setUsername(" bridge ");

        processor.startup();

        assertThat(mqtt.settings.username()).isEqualTo("bridge");
        assertThat(mqtt.settings.password()).isNull();
    }

    @Test
    void aPingAnswerBeforeAnyPingWasSentIsIgnored() {
        processor.startup();

        assertDoesNotThrow(() -> mqtt.deliver(BASE_TOPIC + "/ping", "{}"));
        processor.pingMqtt();
        awaitHealth("mqtt=UP");
    }

    @Test
    void homeAssistantGoingOfflineDoesNotRepublishTheConfig() {
        processor.startup();
        mqtt.clearRecordings();

        assertDoesNotThrow(() -> mqtt.deliver("homeassistant/status", "offline"));

        // Coming online republishes after two to five seconds
        Awaitility.await("no republished config")
                .during(Duration.ofMillis(5500))
                .atMost(Duration.ofSeconds(7))
                .until(mqtt.published::isEmpty);
    }

    @Test
    void anInterruptedStartupSkipsTheWaitsButStillDoesEverythingAndKeepsTheInterrupt() {
        String stale = "homeassistant/switch/fixture_central/relay_998/config";
        retainedOnBroker.put(stale, "{}");

        Thread.currentThread().interrupt();
        processor.startup();

        assertThat(Thread.interrupted()).isTrue();
        assertThat(clearedTopics()).containsExactly(stale);
        assertThat(mqtt.published).extracting(Published::topic).contains(RELAY_1_CONFIG_TOPIC);
        assertThat(mqtt.subscribed).contains(STOP_TOPIC);
    }

    @Test
    void removeConfigClearsTheRetainedConfigOfEveryComponent() {
        configuration.getLog().setHaconfigEnabled(true);
        processor.startup();
        mqtt.clearRecordings();

        processor.removeConfig();

        assertThat(clearedTopics()).containsExactly(RELAY_1_CONFIG_TOPIC, BRIDGE_CONFIG_TOPIC);
    }

    @Test
    void anInterruptedRemoveConfigStillClearsEverythingAndKeepsTheInterrupt() {
        processor.startup();
        mqtt.clearRecordings();

        Thread.currentThread().interrupt();
        processor.removeConfig();

        assertThat(Thread.interrupted()).isTrue();
        assertThat(clearedTopics()).containsExactly(RELAY_1_CONFIG_TOPIC, BRIDGE_CONFIG_TOPIC);
    }

    @Test
    void onlyRetainedConfigAndDiscoveryTopicsCountAsPreviouslyPublished() throws IOException {
        retainedOnBroker.put(DISCOVERY_TOPICS_TOPIC, "");
        liveOnBroker.put(DISCOVERY_TOPICS_TOPIC, "[\"homeassistant/switch/old_central/relay_5/config\"]");
        liveOnBroker.put("homeassistant/switch/fixture_central/relay_996/config", "{}");

        processor.startup();

        assertThat(clearedTopics()).isEmpty();
        assertThat(publishedDiscoveryTopics()).containsExactly(RELAY_1_CONFIG_TOPIC, BRIDGE_CONFIG_TOPIC);
    }

    @Test
    void stoppingTheTeletaskClientKeepsTheMqttConnection() {
        processor.startup();
        mqtt.clearRecordings();

        processor.stop();

        assertThat(mqtt.isConnected()).isTrue();
        assertThat(mqtt.published).isEmpty();
    }

    @Test
    void connectionLostExitsWithStatus5WhenConfigured() {
        configuration.getMqtt().setExitOnDisconnect(true);
        processor.startup();

        mqtt.listener.connectionLost(new IOException("broker gone"));

        assertThat(exitCodes).containsExactly(5);
    }

    @Test
    void connectionLostKeepsRunningByDefault() {
        processor.startup();

        mqtt.listener.connectionLost(new IOException("broker gone"));

        assertThat(exitCodes).isEmpty();
    }

    @Test
    void reconnectSubscribesToEverythingAgainAndThenPublishesTheStatus() {
        processor.startup();
        processor.checkAndPublishConnectedStatus();
        mqtt.clearRecordings();

        mqtt.listener.connected(true);

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

        mqtt.listener.connected(true);

        awaitAvailabilityPublished("online");
        // The reconnect executor survived the failure
        mqtt.failSubscribeTo = null;
        mqtt.clearRecordings();
        mqtt.listener.connected(true);
        Awaitility.await("subscribed again").atMost(AWAIT).untilAsserted(() -> assertThat(mqtt.subscribed).contains(STOP_TOPIC));
    }

    @Test
    void aFailedResubscribeIsTriedAgainWhileTheConnectionStaysUp() {
        processor.setResubscribeDelay(Duration.ofMillis(50));
        processor.startup();
        mqtt.clearRecordings();
        mqtt.failSubscribeTo = BASE_TOPIC + "/+/+/set";

        mqtt.listener.connected(true);
        Awaitility.await("a second attempt").atMost(AWAIT).until(() -> mqtt.failedSubscribes.get() >= 2);
        mqtt.failSubscribeTo = null;

        Awaitility.await("subscribed again").atMost(AWAIT).untilAsserted(() -> assertThat(mqtt.subscribed).contains(STOP_TOPIC));
    }

    @Test
    void aResubscribeThatKeepsFailingIsGivenUpOn() {
        processor.setResubscribeDelay(Duration.ofMillis(10));
        processor.startup();
        mqtt.failSubscribeTo = BASE_TOPIC + "/+/+/set";

        mqtt.listener.connected(true);

        Awaitility.await("five attempts and no more").during(Duration.ofMillis(300)).atMost(AWAIT).until(() -> mqtt.failedSubscribes.get() == 5);
    }

    @Test
    void aResubscribeIsNotTriedAgainOnceTheConnectionIsGone() {
        processor.setResubscribeDelay(Duration.ofMillis(10));
        processor.startup();
        mqtt.failSubscribeTo = BASE_TOPIC + "/+/+/set";
        mqtt.connected = false;

        mqtt.listener.connected(true);

        // The next reconnect subscribes again by itself
        Awaitility.await("one attempt").during(Duration.ofMillis(300)).atMost(AWAIT).until(() -> mqtt.failedSubscribes.get() == 1);
    }

    @Test
    void aResubscribeThatWaitsForTheBrokerDoesNotHoldBackCommands() {
        processor.startup();
        mqtt.blockSubscribes = new CountDownLatch(1);
        try {
            mqtt.listener.connected(true);
            Awaitility.await("resubscribe waiting").atMost(AWAIT).until(() -> mqtt.subscribesWaiting.get() > 0);

            awaitNextGroupGetAfter(() -> mqtt.deliver(REFRESH_TOPIC, "while resubscribing"));
        } finally {
            mqtt.blockSubscribes.countDown();
        }
    }

    @Test
    void aCommandWaitsForTheCentralUnitAndIsExecutedOnceItIsBack() throws Exception {
        processor.startup();
        teletask.connected = false;

        mqtt.deliver(RELAY_1_SET_TOPIC, "{\"state\":\"ON\"}");
        Awaitility.await("not executed while away").during(Duration.ofMillis(300)).atMost(AWAIT).until(() -> teletask.commandsReceived.get() == 0);

        teletask.connected = true;
        Awaitility.await("executed").atMost(AWAIT).until(() -> teletask.commandsReceived.get() == 1);
    }

    @Test
    void aCommandIsNotExecutedWhenTheCentralUnitStaysAwayLongerThanItWaits() throws Exception {
        processor.setCommandMaxWait(Duration.ofMillis(200));
        processor.startup();
        teletask.connected = false;

        mqtt.deliver(RELAY_1_SET_TOPIC, "{\"state\":\"ON\"}");
        // Behind the command on the same executor, so it ends after the command gave up
        awaitNextGroupGetAfter(() -> mqtt.deliver(REFRESH_TOPIC, "sentinel"));
        teletask.connected = true;

        awaitNextGroupGetAfter(() -> mqtt.deliver(REFRESH_TOPIC, "sentinel"));
        assertThat(teletask.commandsReceived).hasValue(0);
    }

    @Test
    void theMqttClientStopsWaitingForAnAnswerOfTheBroker() {
        processor.startup();

        assertThat(mqtt.settings.timeToWait()).isEqualTo(MqttProcessor.MQTT_TIME_TO_WAIT);
    }

    @Test
    void firstConnectDoesNotSubscribeASecondTime() {
        processor.startup();
        assertThat(mqtt.subscribed).containsOnlyOnce(STOP_TOPIC);
        mqtt.clearRecordings();

        mqtt.listener.connected(false);

        assertDoesNotThrow(() -> MqttProcessor.afterReconnectWork().get(AWAIT.toSeconds(), TimeUnit.SECONDS));
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
    void aFailedPeriodicRefreshDoesNotEndTheRefreshes() {
        configuration.getPublish().setStatesInterval(1);
        processor.startup();
        teletask.groupGetFails = true;
        int before = teletask.groupGets.get();

        Awaitility.await("refreshes after the first failure").atMost(AWAIT).until(() -> teletask.groupGets.get() >= before + 2);
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
     * Stands in for the MQTT client and a broker: records what the bridge does, and hands published messages to
     * matching subscribers on the calling thread.
     */
    private final class FakeMqttClient implements MqttConnection {
        final List<String> subscribed = new CopyOnWriteArrayList<>();
        final List<String> publishAttempts = new CopyOnWriteArrayList<>();
        final List<Published> published = new CopyOnWriteArrayList<>();
        private final Map<String, MessageHandler> handlers = new ConcurrentHashMap<>();
        volatile Listener listener;
        volatile MqttConnectionSettings settings;
        volatile boolean connected;
        volatile boolean echoPublishes = true;
        volatile String failSubscribeTo;
        volatile CountDownLatch blockSubscribes;
        volatile MqttConnectionException failConnect;
        final AtomicInteger subscribesWaiting = new AtomicInteger();
        final AtomicInteger failedSubscribes = new AtomicInteger();
        volatile String failPublishTo;

        void clearRecordings() {
            subscribed.clear();
            publishAttempts.clear();
            published.clear();
        }

        @Override
        public void connect(MqttConnectionSettings settings, Listener listener) throws MqttConnectionException {
            if (failConnect != null) {
                throw failConnect;
            }
            this.settings = settings;
            this.listener = listener;
            connected = true;
            listener.connected(false);
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public void subscribe(String topicFilter, MessageHandler handler) throws MqttConnectionException {
            if (topicFilter.equals(failSubscribeTo)) {
                failedSubscribes.incrementAndGet();
                throw new MqttConnectionException("The broker refused the subscription to " + topicFilter);
            }
            awaitSubscribeUnblocked();
            subscribed.add(topicFilter);
            handlers.put(topicFilter, handler);
            deliverOnSubscribe(topicFilter, handler, retainedOnBroker, true);
            deliverOnSubscribe(topicFilter, handler, liveOnBroker, false);
        }

        // Like a SUBACK that is slow to come
        private void awaitSubscribeUnblocked() throws MqttConnectionException {
            CountDownLatch block = blockSubscribes;
            if (block == null) {
                return;
            }
            subscribesWaiting.incrementAndGet();
            try {
                // Longer than any test waits, so a held back command times the test out first
                if (!block.await(AWAIT.toSeconds() * 3, TimeUnit.SECONDS)) {
                    throw new MqttConnectionException("No answer of the broker");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MqttConnectionException("Interrupted", e);
            } finally {
                subscribesWaiting.decrementAndGet();
            }
        }

        private static void deliverOnSubscribe(String topicFilter, MessageHandler handler, Map<String, String> messages, boolean retained) {
            for (Map.Entry<String, String> entry : messages.entrySet()) {
                if (MqttTopic.isMatched(topicFilter, entry.getKey())) {
                    handler.arrived(entry.getKey(), new Message(entry.getValue().getBytes(), retained));
                }
            }
        }

        @Override
        public void unsubscribe(String... topicFilters) {
            for (String topicFilter : topicFilters) {
                handlers.remove(topicFilter);
            }
        }

        @Override
        public void publish(String topic, byte[] payload, boolean retained) throws MqttConnectionException {
            publishAttempts.add(topic);
            if (topic.equals(failPublishTo)) {
                throw new MqttConnectionException("Not connected");
            }
            published.add(new Published(topic, new String(payload), retained));
            if (echoPublishes) {
                deliver(topic, new Message(payload, false));
            }
        }

        @Override
        public void close() {
            connected = false;
        }

        void deliver(String topic, String payload) {
            deliver(topic, Message.of(payload));
        }

        private void deliver(String topic, Message message) {
            for (Map.Entry<String, MessageHandler> handler : handlers.entrySet()) {
                if (MqttTopic.isMatched(handler.getKey(), topic)) {
                    handler.getValue().arrived(topic, message);
                }
            }
        }
    }
}
