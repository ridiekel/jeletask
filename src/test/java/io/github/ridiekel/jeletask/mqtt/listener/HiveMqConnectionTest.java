package io.github.ridiekel.jeletask.mqtt.listener;

import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties.MqttConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * What the bridge relies on from its MQTT client, beyond what the library promises by itself.
 */
class HiveMqConnectionTest {
    private static final MqttConnection.Listener IGNORE = new MqttConnection.Listener() {
        @Override
        public void connected(boolean reconnect) {
            // Not what these tests are about
        }

        @Override
        public void connectionLost(Throwable cause) {
            // Not what these tests are about
        }
    };

    private final List<MqttConnection> connections = new ArrayList<>();

    @AfterEach
    void closeConnections() {
        this.connections.forEach(MqttConnection::close);
    }

    @Test
    void theFirstConnectFailsRightAwayAndDoesNotKeepTrying() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            closedPort = socket.getLocalPort();
        }
        MqttConnection connection = this.connection();
        long start = System.nanoTime();

        assertThatThrownBy(() -> connection.connect(MqttTlsTest.settings(config("127.0.0.1", closedPort), "unreachable"), IGNORE))
                .isInstanceOf(MqttConnectionException.class)
                .hasMessageContaining("Could not connect");

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
        assertThat(connection.isConnected()).isFalse();
    }

    private MqttConnection connection() {
        MqttConnection connection = new HiveMqConnection();
        this.connections.add(connection);
        return connection;
    }

    private static MqttConfiguration config(String host, int port) {
        MqttConfiguration mqtt = new MqttConfiguration();
        mqtt.setHost(host);
        mqtt.setPort(String.valueOf(port));
        return mqtt;
    }

    static boolean dockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    @Nested
    @EnabledIf("io.github.ridiekel.jeletask.mqtt.listener.HiveMqConnectionTest#dockerAvailable")
    class AgainstABroker {
        private static GenericContainer<?> broker;

        @BeforeAll
        static void startBroker() {
            broker = new GenericContainer<>(DockerImageName.parse("eclipse-mosquitto:2.1.2-alpine"))
                    .withExposedPorts(1883)
                    .withCommand("mosquitto", "-c", "/mosquitto-no-auth.conf")
                    .waitingFor(Wait.forListeningPort())
                    .withStartupTimeout(Duration.ofMinutes(5));
            broker.start();
        }

        @AfterAll
        static void stopBroker() {
            broker.stop();
        }

        @Test
        void subscribingAgainDoesNotDeliverTwice() throws MqttConnectionException {
            MqttConnection bridge = connected("twice-bridge");
            List<String> received = new CopyOnWriteArrayList<>();
            bridge.subscribe("hivemq-test/twice", (_, message) -> received.add("first " + message));
            // Like the resubscribe after a reconnect
            bridge.subscribe("hivemq-test/twice", (_, message) -> received.add("second " + message));

            connected("twice-other").publish("hivemq-test/twice", "on".getBytes(), false);

            await().atMost(10, TimeUnit.SECONDS).until(() -> !received.isEmpty());
            // Anything delivered twice would arrive right behind the first
            await().pollDelay(500, TimeUnit.MILLISECONDS).atMost(1, TimeUnit.SECONDS).until(() -> true);
            assertThat(received).containsExactly("second on");
        }

        @Test
        void aFailingHandlerDoesNotCostTheMessagesAfterIt() throws MqttConnectionException {
            MqttConnection bridge = connected("failing-bridge");
            List<String> received = new CopyOnWriteArrayList<>();
            bridge.subscribe("hivemq-test/failing", (_, message) -> {
                if (message.toString().equals("bad")) {
                    throw new IllegalStateException("a bad message");
                }
                received.add(message.toString());
            });

            MqttConnection other = connected("failing-other");
            other.publish("hivemq-test/failing", "bad".getBytes(), false);
            other.publish("hivemq-test/failing", "good".getBytes(), false);

            await().atMost(10, TimeUnit.SECONDS).until(() -> received.contains("good"));
        }

        @Test
        void retainedMessagesArriveMarkedAsRetained() throws MqttConnectionException {
            connected("retained-other").publish("hivemq-test/retained", "kept".getBytes(), true);
            MqttConnection bridge = connected("retained-bridge");
            List<MqttConnection.Message> received = new CopyOnWriteArrayList<>();

            bridge.subscribe("hivemq-test/retained", (_, message) -> received.add(message));

            await().atMost(10, TimeUnit.SECONDS).until(() -> !received.isEmpty());
            assertThat(received.getFirst().isRetained()).isTrue();
            assertThat(received.getFirst()).hasToString("kept");
        }

        private MqttConnection connected(String clientId) throws MqttConnectionException {
            MqttConnection connection = connection();
            connection.connect(MqttTlsTest.settings(config(broker.getHost(), broker.getMappedPort(1883)), clientId), IGNORE);
            return connection;
        }
    }
}
