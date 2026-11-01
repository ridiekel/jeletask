package io.github.ridiekel.jeletask.mqtt.listener;

import io.github.ridiekel.jeletask.HealthCheck;
import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.ClientThreads;
import io.github.ridiekel.jeletask.client.TeletaskClientImpl;
import io.github.ridiekel.jeletask.client.TestCentralUnit;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator.ValidOnOffToggle;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;
import io.github.ridiekel.jeletask.mockserver.FakeCentral;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.awaitility.core.ConditionFactory;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * The real client and the real processor wired like Spring wires them, connected through a {@link ResilienceNetwork}.
 * What they do is only read from the outside: the MQTT observer, the bytes the central unit gets and the health file.
 */
final class ResilienceBridge implements AutoCloseable {
    static final int RELAYS = 8;
    static final Set<Function> LOGGED_FUNCTIONS = Set.of(Function.RELAY, Function.LOCMOOD, Function.GENMOOD, Function.MOTOR, Function.DIMMER, Function.COND, Function.SENSOR, Function.FLAG, Function.TIMEDFNC, Function.INPUT);

    private static final Duration AWAIT = Duration.ofSeconds(90);
    private static final int KEEP_ALIVE_MILLIS = 1000;
    private static final long STATUS_INTERVAL_MILLIS = 250;
    private static final int FRAME_NUMBER_HIGH_INDEX = 5;
    private static final int FRAME_NUMBER_LOW_INDEX = 6;
    private static final int FRAME_STATE_INDEX = 7;

    private final ClientThreads threads = new ClientThreads();
    private final FakeCentral central;
    private final TestCentralUnit unit;
    private final TeletaskClientImpl client;
    private final MqttProcessor processor;
    private final String baseTopic;
    private final String observerBrokerUrl;
    private final Map<Integer, ValidOnOffToggle> relaysOnCentral = new ConcurrentHashMap<>();
    private final List<Seen> seen = new CopyOnWriteArrayList<>();
    // Spring runs the status check every 5 seconds, this keeps a scenario from waiting for it
    private final ScheduledExecutorService status = Executors.newSingleThreadScheduledExecutor(runnable -> new Thread(runnable, "resilience-status"));
    private MqttClient bridgeMqtt;
    private MqttClient observer;

    ResilienceBridge(FakeCentral central, ResilienceNetwork network) {
        this.central = central;
        this.observerBrokerUrl = network.observerBrokerUrl();
        this.unit = TestCentralUnit.withKeepAliveEvery(network.proxiedCentralPort(), KEEP_ALIVE_MILLIS);
        this.unit.setHost(network.host());
        this.unit.setComponentsTypes(Map.of(Function.RELAY, IntStream.rangeClosed(1, RELAYS).mapToObj(ResilienceBridge::relayOff).toList()));

        Teletask2MqttConfigurationProperties configuration = MqttProcessorFixtures.configuration();
        configuration.getMqtt().setHost(network.host());
        configuration.getMqtt().setPort(String.valueOf(network.proxiedBrokerPort()));

        this.client = new TeletaskClientImpl(this.unit);
        this.processor = new MqttProcessor(this.unit, this.client, configuration, _ -> {
            // Nothing asks this bridge to exit, a scenario would notice it stops answering
        }, (brokerUrl, clientId) -> this.bridgeMqtt = new MqttClient(brokerUrl, clientId, new MemoryPersistence()));
        this.baseTopic = MqttProcessorFixtures.BASE_TOPIC;

        central.respondWith(this::respond);
    }

    void start() throws MqttException {
        this.observer = new MqttClient(this.observerBrokerUrl, "resilience-observer", new MemoryPersistence());
        this.observer.connect();
        this.observer.subscribe(this.baseTopic + "/#", 1, (topic, message) -> this.seen.add(new Seen(topic, new String(message.getPayload()))));

        this.processor.startup();
        this.status.scheduleWithFixedDelay(this::checkStatus, 0, STATUS_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);

        awaitHealth("teletask=UP, mqtt=UP");
        awaitAvailability("online");
        awaitStatusRequestsOnLatestConnection();
        this.seen.clear();
    }

    /**
     * Waits without taking over the exceptions of other threads, see {@link ClientThreads}.
     */
    static ConditionFactory eventually() {
        return ClientThreads.eventually().atMost(AWAIT).pollInterval(Duration.ofMillis(50));
    }

    int connections() {
        return this.central.connectionCount();
    }

    int keepAlives() {
        return this.central.frames(Command.KEEP_ALIVE).size();
    }

    TestCentralUnit unit() {
        return this.unit;
    }

    void centralSends(byte[] bytes) {
        this.central.send(bytes);
    }

    /**
     * As if someone uses a wall switch: the central unit knows, and tells the bridge if it can.
     */
    void relayChangesOnCentral(int relay, ValidOnOffToggle state) {
        this.relaysOnCentral.put(relay, state);
        this.central.send(event(relay, state));
    }

    /**
     * The same, on a line that is down: the bridge can only learn about this by asking.
     */
    void relayChangesOnCentralWhileAway(int relay, ValidOnOffToggle state) {
        this.relaysOnCentral.put(relay, state);
    }

    void publishSet(int relay, ValidOnOffToggle state) throws MqttException {
        this.observer.publish(this.baseTopic + "/relay/" + relay + "/set", ("{\"state\":\"" + state + "\"}").getBytes(), 1, false);
    }

    int setFramesFor(int relay, ValidOnOffToggle state) {
        return (int) this.central.frames(Command.SET).stream()
                .filter(frame -> number(frame) == relay && stateOf(frame) == state)
                .count();
    }

    int setFrames() {
        return this.central.frames(Command.SET).size();
    }

    void forgetSeen() {
        this.seen.clear();
    }

    boolean sawOnMqtt(int relay, ValidOnOffToggle state) {
        String topic = this.baseTopic + "/relay/" + relay + "/state";
        return this.seen.stream().anyMatch(message -> message.topic().equals(topic) && message.payload().contains("\"state\":\"" + state + "\""));
    }

    boolean sawAvailability(String availability) {
        String topic = this.processor.getAvailabilityTopic();
        return this.seen.stream().anyMatch(message -> message.topic().equals(topic) && message.payload().equals(availability));
    }

    void awaitOnMqtt(int relay, ValidOnOffToggle state) {
        eventually().alias("relay " + relay + " " + state + " on MQTT").until(() -> sawOnMqtt(relay, state));
    }

    void awaitAvailability(String availability) {
        eventually().alias("availability " + availability).until(() -> availability.equals(lastAvailability()));
    }

    void awaitHealth(String expected) {
        eventually().alias("health " + expected).until(() -> health().contains(expected));
    }

    /**
     * What the client asks a central unit for on every new connection. Waits for the connection count to settle on the
     * one the client stays on, so an earlier attempt does not count.
     */
    void awaitStatusRequestsOnLatestConnection() {
        eventually().alias("LOG and GROUPGET on the latest connection").until(() -> {
            List<FakeCentral.Frame> frames = this.central.frames(this.central.connectionCount() - 1);
            return this.client.isConnected()
                    && frames.stream().anyMatch(frame -> frame.command() == Command.GROUPGET && frame.function() == Function.RELAY)
                    && frames.stream().filter(frame -> frame.command() == Command.LOG).map(FakeCentral.Frame::function).toList().containsAll(LOGGED_FUNCTIONS);
        });
    }

    /**
     * The whole way there and back: an MQTT command reaches the central unit as bytes, its answer ends up on MQTT.
     */
    void awaitCommandRoundTrip(int relay, ValidOnOffToggle state) throws MqttException {
        publishSet(relay, state);
        eventually().alias("SET of relay " + relay + " on the central unit").until(() -> setFramesFor(relay, state) > 0);
        awaitOnMqtt(relay, state);
    }

    @Override
    public void close() throws MqttException, InterruptedException {
        this.status.shutdownNow();
        this.status.awaitTermination(10, TimeUnit.SECONDS);
        try {
            close(this.observer);
            this.client.disconnect();
            // A test has to outlive its client, see ClientThreads
            this.threads.awaitAllGone();
            // The connected status is static, hand it back like MqttProcessorTest does
            this.processor.checkAndPublishConnectedStatus();
        } finally {
            close(this.bridgeMqtt);
        }
    }

    private String lastAvailability() {
        String topic = this.processor.getAvailabilityTopic();
        return this.seen.stream().filter(message -> message.topic().equals(topic)).map(Seen::payload).reduce((_, last) -> last).orElse(null);
    }

    private static String health() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HealthCheck.getDefault().check(new PrintStream(out));
        return out.toString();
    }

    private void checkStatus() {
        this.processor.checkAndPublishConnectedStatus();
    }

    private byte[] respond(FakeCentral.Frame frame) {
        return switch (frame.command()) {
            case SET -> {
                ValidOnOffToggle state = stateOf(frame);
                this.relaysOnCentral.put(number(frame), state);
                yield event(number(frame), state);
            }
            case GROUPGET -> frame.function() == Function.RELAY ? allRelayEvents() : new byte[0];
            default -> new byte[0];
        };
    }

    private byte[] allRelayEvents() {
        byte[] events = new byte[0];
        for (int relay = 1; relay <= RELAYS; relay++) {
            events = Bytes.concat(events, event(relay, this.relaysOnCentral.getOrDefault(relay, ValidOnOffToggle.OFF)));
        }
        return events;
    }

    private byte[] event(int relay, ValidOnOffToggle state) {
        return this.unit.event(Function.RELAY, relay, new OnOffState(state));
    }

    private static ComponentSpec relayOff(int number) {
        return new ComponentSpec(Function.RELAY, new OnOffState(ValidOnOffToggle.OFF), number);
    }

    private static int number(FakeCentral.Frame frame) {
        return (frame.bytes()[FRAME_NUMBER_HIGH_INDEX] & 0xFF) << 8 | frame.bytes()[FRAME_NUMBER_LOW_INDEX] & 0xFF;
    }

    private static ValidOnOffToggle stateOf(FakeCentral.Frame frame) {
        return frame.bytes()[FRAME_STATE_INDEX] == 0 ? ValidOnOffToggle.OFF : ValidOnOffToggle.ON;
    }

    private static void close(MqttClient mqtt) throws MqttException {
        if (mqtt != null) {
            mqtt.disconnectForcibly(0, 0, false);
            mqtt.close(true);
        }
    }

    private record Seen(String topic, String payload) {
    }
}
