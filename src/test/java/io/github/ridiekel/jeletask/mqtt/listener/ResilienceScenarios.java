package io.github.ridiekel.jeletask.mqtt.listener;

import ch.qos.logback.classic.Level;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.model.Toxic;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import eu.rekawek.toxiproxy.model.toxic.Slicer;
import io.github.ridiekel.jeletask.client.TeletaskClientImpl;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator.ValidOnOffToggle;
import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;
import io.github.ridiekel.jeletask.mockserver.FakeCentral;
import io.github.ridiekel.jeletask.mockserver.LogCapture;
import io.github.ridiekel.jeletask.mqtt.listener.command.MQTTMessageToTeletaskCommand;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static io.github.ridiekel.jeletask.mqtt.listener.ResilienceBridge.eventually;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The bridge on a real TCP line that a real Toxiproxy disturbs. Everything is judged from the outside: what reaches the
 * MQTT observer, the bytes the central unit gets and the health file.
 * <p>
 * Runs through {@link ResilienceToxiproxyTest}: this class only knows there is a {@link ResilienceNetwork}.
 */
@Timeout(value = 5, unit = TimeUnit.MINUTES)
abstract class ResilienceScenarios {
    private static final String ONLINE = "online";
    private static final String OFFLINE = "offline";
    private static final String TELETASK_DOWN_MQTT_UP = "teletask=DOWN, mqtt=UP";
    private static final String TELETASK_UP_MQTT_DOWN = "teletask=UP, mqtt=DOWN";
    private static final String ALL_UP = "teletask=UP, mqtt=UP";
    // The real limits are MessageSupport.ACK_WAIT_TIME (2 s) and the 500 ms of silence in MessageUtilities
    private static final long LATENCY_WITHIN_LIMITS_MILLIS = 300;
    private static final long LATENCY_BEYOND_ACK_MILLIS = MessageSupport.ACK_WAIT_TIME * 2L;
    private static final long CENTRAL_STARTUP_WAIT_SECONDS = 30;

    private LogCapture quietClient;
    private LogCapture quietProcessor;
    private LogCapture quietCommands;
    private FakeCentral central;
    private ResilienceNetwork network;
    private ResilienceBridge bridge;

    abstract ResilienceNetwork startNetwork(int centralPort) throws IOException;

    @BeforeEach
    void setUp() throws IOException, MqttException {
        this.quietClient = LogCapture.quietClient();
        this.quietProcessor = LogCapture.of(MqttProcessor.class, Level.INFO);
        this.quietCommands = LogCapture.of(MQTTMessageToTeletaskCommand.class, Level.INFO);
        this.central = new FakeCentral().goOnline();
        this.network = startNetwork(this.central.port());
        this.bridge = new ResilienceBridge(this.central, this.network);
        this.bridge.start();
    }

    @AfterEach
    void tearDown() throws MqttException, InterruptedException {
        try {
            if (this.bridge != null) {
                this.bridge.close();
            }
        } finally {
            if (this.network != null) {
                this.network.close();
            }
            this.central.close();
            this.quietCommands.close();
            this.quietProcessor.close();
            this.quietClient.close();
        }
    }

    @Test
    void eventsThatArriveInSmallPiecesAreAllPublished() throws IOException {
        ValidOnOffToggle on = ValidOnOffToggle.ON;
        byte[] burst = IntStream.rangeClosed(1, ResilienceBridge.RELAYS)
                .mapToObj(relay -> this.bridge.unit().event(Function.RELAY, relay, new OnOffState(on)))
                .reduce(new byte[0], Bytes::concat);
        // The fragments sit far below the 500 ms of silence that counts as a lost frame
        Slicer slicer = this.network.centralLine().toxics().slicer("pieces", ToxicDirection.DOWNSTREAM, 2, TimeUnit.MILLISECONDS.toMicros(20));
        slicer.setSizeVariation(1);

        try (LogCapture listenerLog = LogCapture.of(TeletaskClientImpl.class, Level.TRACE);
             LogCapture acknowledgeLog = LogCapture.of(MessageSupport.class, Level.TRACE)) {
            this.bridge.centralSends(burst);

            IntStream.rangeClosed(1, ResilienceBridge.RELAYS).forEach(relay -> this.bridge.awaitOnMqtt(relay, on));

            // Without this, a slicer that lets everything through in one go would also pass
            assertThat(listenerLog.messages(Level.TRACE).stream().filter(ResilienceScenarios::isOverflow).count()
                    + acknowledgeLog.messages(Level.TRACE).stream().filter(ResilienceScenarios::isOverflow).count())
                    .isGreaterThan(1);
        }
        assertThat(this.bridge.connections()).isEqualTo(1);
    }

    @Test
    void latencyWithinTheLimitsChangesNothing() throws IOException, MqttException {
        Proxy line = this.network.centralLine();
        line.toxics().latency("slow-down", ToxicDirection.DOWNSTREAM, LATENCY_WITHIN_LIMITS_MILLIS);
        line.toxics().latency("slow-up", ToxicDirection.UPSTREAM, LATENCY_WITHIN_LIMITS_MILLIS);
        int keepAlives = this.bridge.keepAlives();

        long before = System.nanoTime();
        this.bridge.relayChangesOnCentral(1, ValidOnOffToggle.ON);
        this.bridge.awaitOnMqtt(1, ValidOnOffToggle.ON);
        // Only a lower bound: proves the toxic is really in the way
        assertThat(Duration.ofNanos(System.nanoTime() - before)).isGreaterThanOrEqualTo(Duration.ofMillis(LATENCY_WITHIN_LIMITS_MILLIS));

        this.bridge.awaitCommandRoundTrip(2, ValidOnOffToggle.ON);
        eventually().alias("a few keep-alives through the latency").until(() -> this.bridge.keepAlives() >= keepAlives + 3);

        assertThat(this.bridge.connections()).isEqualTo(1);
        assertThat(this.bridge.sawAvailability(OFFLINE)).isFalse();
        this.bridge.awaitHealth(ALL_UP);
    }

    @Test
    void latencyBeyondTheAcknowledgeTimeoutIsDetectedAndRecoveredFrom() throws IOException, MqttException {
        Toxic latency = this.network.centralLine().toxics().latency("too-slow", ToxicDirection.DOWNSTREAM, LATENCY_BEYOND_ACK_MILLIS);

        this.bridge.awaitHealth(TELETASK_DOWN_MQTT_UP);
        this.bridge.awaitAvailability(OFFLINE);

        latency.remove();

        this.bridge.awaitHealth(ALL_UP);
        this.bridge.awaitAvailability(ONLINE);
        this.bridge.awaitStatusRequestsOnLatestConnection();
        assertThat(this.bridge.connections()).isGreaterThan(1);
        this.bridge.awaitCommandRoundTrip(1, ValidOnOffToggle.ON);
    }

    @Test
    void aResetLineIsReconnectedAndStatusAndCommandsWorkAgain() throws IOException, MqttException {
        Toxic reset = this.network.centralLine().toxics().resetPeer("reset", ToxicDirection.DOWNSTREAM, 0);

        this.bridge.awaitHealth(TELETASK_DOWN_MQTT_UP);
        this.bridge.awaitAvailability(OFFLINE);

        reset.remove();

        this.bridge.awaitHealth(ALL_UP);
        this.bridge.awaitAvailability(ONLINE);
        this.bridge.awaitStatusRequestsOnLatestConnection();
        assertThat(this.bridge.connections()).isGreaterThan(1);
        this.bridge.awaitCommandRoundTrip(1, ValidOnOffToggle.ON);
    }

    @Test
    void aHalfDeadLineIsNoticedByTheKeepAliveAndRecoveredFrom() throws IOException, MqttException {
        // Timeout 0: nothing gets through and nobody closes anything, like a cable that is gone without a sound
        Proxy line = this.network.centralLine();
        Toxic down = line.toxics().timeout("dead-down", ToxicDirection.DOWNSTREAM, 0);
        Toxic up = line.toxics().timeout("dead-up", ToxicDirection.UPSTREAM, 0);

        this.bridge.awaitHealth(TELETASK_DOWN_MQTT_UP);
        this.bridge.awaitAvailability(OFFLINE);

        up.remove();
        down.remove();

        this.bridge.awaitHealth(ALL_UP);
        this.bridge.awaitAvailability(ONLINE);
        this.bridge.awaitStatusRequestsOnLatestConnection();
        this.bridge.forgetSeen();
        this.bridge.relayChangesOnCentral(1, ValidOnOffToggle.ON);
        this.bridge.awaitOnMqtt(1, ValidOnOffToggle.ON);
        this.bridge.awaitCommandRoundTrip(2, ValidOnOffToggle.ON);
    }

    @Test
    void theCentralLineGoingDownLeavesMqttAloneAndTheBridgeCatchesUpAfterwards() throws IOException, MqttException {
        Proxy line = this.network.centralLine();
        line.disable();

        this.bridge.awaitHealth(TELETASK_DOWN_MQTT_UP);
        this.bridge.awaitAvailability(OFFLINE);
        this.bridge.relayChangesOnCentralWhileAway(3, ValidOnOffToggle.ON);

        line.enable();

        this.bridge.awaitHealth(ALL_UP);
        this.bridge.awaitAvailability(ONLINE);
        this.bridge.awaitStatusRequestsOnLatestConnection();
        this.bridge.awaitOnMqtt(3, ValidOnOffToggle.ON);
        this.bridge.awaitCommandRoundTrip(1, ValidOnOffToggle.ON);
    }

    @Test
    void theBrokerLineGoingDownLeavesTheCentralAloneAndTheBridgeSubscribesAgain() throws IOException, MqttException {
        int connections = this.bridge.connections();
        int subscribes = subscribesLogged();
        Proxy line = this.network.brokerLine();
        line.disable();

        this.bridge.awaitHealth(TELETASK_UP_MQTT_DOWN);
        // The last will: the broker saw the line go, the bridge did not say goodbye
        this.bridge.awaitAvailability(OFFLINE);

        line.enable();

        this.bridge.awaitHealth(ALL_UP);
        this.bridge.awaitAvailability(ONLINE);
        // The broker kept the session, so a working command alone does not show the bridge subscribed again
        eventually().alias("subscribed again").until(() -> subscribesLogged() > subscribes);
        this.bridge.awaitCommandRoundTrip(1, ValidOnOffToggle.ON);
        assertThat(this.bridge.connections()).isEqualTo(connections);
    }

    /**
     * Characterization, not a wish: an MQTT command during a short outage waits for the reconnect.
     */
    @Test
    void characterizationACommandDuringAShortOutageWaitsAndIsThenExecuted() throws IOException, MqttException {
        Proxy line = this.network.centralLine();
        line.disable();
        awaitClientNoticed();

        this.bridge.publishSet(1, ValidOnOffToggle.ON);
        eventually().alias("command picked up").until(() -> !this.quietCommands.messages(Level.INFO).isEmpty());
        assertThat(this.bridge.setFrames()).isZero();

        line.enable();

        eventually().alias("SET after the reconnect").until(() -> this.bridge.setFramesFor(1, ValidOnOffToggle.ON) == 1);
        this.bridge.awaitOnMqtt(1, ValidOnOffToggle.ON);
    }

    /**
     * Characterization, not a wish: see TeletaskClientImpl.withStarted. A command waits 30 seconds for the client and is
     * then gone, without anyone being told over MQTT and without a retry after the reconnect.
     */
    @Test
    void characterizationACommandDuringALongOutageIsLostAfterThirtySeconds() throws IOException, MqttException {
        Proxy line = this.network.centralLine();
        line.disable();
        awaitClientNoticed();

        long before = System.nanoTime();
        this.bridge.publishSet(1, ValidOnOffToggle.ON);
        eventually().alias("command given up on").until(() -> !this.quietCommands.messages(Level.WARN).isEmpty());

        assertThat(Duration.ofNanos(System.nanoTime() - before)).isGreaterThanOrEqualTo(Duration.ofSeconds(CENTRAL_STARTUP_WAIT_SECONDS));
        assertThat(this.quietCommands.messages(Level.WARN)).singleElement().asString().contains("Teletask Client Startup");

        line.enable();

        this.bridge.awaitHealth(ALL_UP);
        this.bridge.awaitStatusRequestsOnLatestConnection();
        // A later command gets through and goes first: the lost one is not waiting anywhere
        this.bridge.awaitCommandRoundTrip(2, ValidOnOffToggle.ON);
        assertThat(this.bridge.setFramesFor(1, ValidOnOffToggle.ON)).isZero();
        assertThat(this.bridge.sawOnMqtt(1, ValidOnOffToggle.ON)).isFalse();
    }

    /**
     * Health alone would not do here: the file has a fixed path, so another test run on this machine can write DOWN too.
     * A command sent before the client noticed is a different story, it goes down the dead line.
     */
    private void awaitClientNoticed() {
        this.bridge.awaitHealth(TELETASK_DOWN_MQTT_UP);
        this.bridge.awaitAvailability(OFFLINE);
    }

    private int subscribesLogged() {
        return (int) this.quietProcessor.messages(Level.INFO).stream().filter(line -> line.contains("SUBSCRIBE") && line.contains("/+/+/set")).count();
    }

    private static boolean isOverflow(String line) {
        return line.startsWith("Receive - Overflowing");
    }
}
