package io.github.ridiekel.jeletask.end2end;

import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties.MqttConfiguration;
import io.github.ridiekel.jeletask.mqtt.listener.HiveMqConnection;
import io.github.ridiekel.jeletask.mqtt.listener.MqttConnection;
import io.github.ridiekel.jeletask.mqtt.listener.MqttConnectionSettings;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A lost connection to a broker that wants a login. The client builds a new CONNECT for every automatic reconnect, and
 * until 7.1.2 that one carried neither the login nor the last will: they were only on the first CONNECT. A broker
 * without anonymous access then refused the bridge with "not authorised" for as long as it ran, measured on
 * 2026-11-18 after the broker of a house was replaced. The broker in the other reconnect test allows anonymous
 * clients, which is why it never showed.
 */
class End2EndReconnectWithLoginTest {
    private static final String USER = "bridge";
    private static final String PASSWORD = "bridge-secret";
    private static final String WILL_TOPIC = "reconnect-login-test/state";
    private static final Duration AWAIT = Duration.ofSeconds(60);

    private static final String CONFIG = """
            listener 1883
            allow_anonymous false
            password_file /mosquitto/config/passwd
            """;

    private static GenericContainer<?> broker;

    @BeforeAll
    static void startBroker() {
        broker = new GenericContainer<>(DockerImageName.parse("eclipse-mosquitto:latest"))
                .withExposedPorts(1883)
                .withCopyToContainer(Transferable.of(CONFIG), "/mosquitto/config/login.conf")
                .withCommand("sh", "-c", "mosquitto_passwd -b -c /mosquitto/config/passwd " + USER + " " + PASSWORD
                        + " && chown mosquitto /mosquitto/config/passwd"
                        + " && exec mosquitto -c /mosquitto/config/login.conf")
                .waitingFor(Wait.forLogMessage(".*mosquitto version .* running.*", 1))
                .withStartupTimeout(Duration.ofMinutes(5));
        broker.start();
    }

    @AfterAll
    static void stopBroker() {
        if (broker != null) {
            broker.stop();
        }
    }

    @Test
    void afterALostConnectionTheBridgeLogsInAgainAndKeepsItsLastWill() throws Exception {
        List<String> state = new CopyOnWriteArrayList<>();
        MqttClient observer = observer(state);
        AtomicInteger reconnects = new AtomicInteger();
        MqttConnection bridge = new HiveMqConnection();
        try (TcpRelay relay = new TcpRelay(broker.getHost(), broker.getMappedPort(1883))) {
            bridge.connect(settings(relay), new MqttConnection.Listener() {
                @Override
                public void connected(boolean reconnect) {
                    if (reconnect) {
                        reconnects.incrementAndGet();
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    // Expected: the relay drops the connection
                }
            });

            relay.cut();
            Awaitility.await("reconnected with the login").atMost(AWAIT).until(() -> reconnects.get() == 1);

            // The will of the first connection went out with the first cut; this one must come from the reconnect
            observer.publish(WILL_TOPIC, "online".getBytes(), 1, true);
            awaitLast(state, "online");
            relay.cut();
            awaitLast(state, "offline");
        } finally {
            bridge.close();
            observer.disconnectForcibly(0, 0, false);
            observer.close(true);
        }
    }

    private static MqttConnectionSettings settings(TcpRelay relay) {
        MqttConfiguration mqtt = new MqttConfiguration();
        mqtt.setHost(relay.host());
        mqtt.setPort(String.valueOf(relay.port()));
        return new MqttConnectionSettings(mqtt, "reconnect-login-test-bridge", USER, PASSWORD, Duration.ofSeconds(10), Duration.ofSeconds(5),
                Duration.ofMillis(200), Duration.ofSeconds(1), Duration.ofSeconds(10), WILL_TOPIC, "offline");
    }

    private static MqttClient observer(List<String> state) throws MqttException {
        MqttClient observer = new MqttClient("tcp://" + broker.getHost() + ":" + broker.getMappedPort(1883), "reconnect-login-test-observer", new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(USER);
        options.setPassword(PASSWORD.toCharArray());
        observer.connect(options);
        observer.subscribe(WILL_TOPIC, 1, (_, message) -> state.add(new String(message.getPayload())));
        return observer;
    }

    private static void awaitLast(List<String> received, String expected) {
        Awaitility.await("state " + expected)
                .pollInterval(250, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> !received.isEmpty() && Objects.equals(received.getLast(), expected));
    }
}
