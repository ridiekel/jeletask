package io.github.ridiekel.jeletask.end2end;

import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties.MqttConfiguration;
import com.hivemq.client.mqtt.mqtt3.exceptions.Mqtt3ConnAckException;
import io.github.ridiekel.jeletask.mqtt.listener.HiveMqConnection;
import io.github.ridiekel.jeletask.mqtt.listener.MqttConnection;
import io.github.ridiekel.jeletask.mqtt.listener.MqttConnectionException;
import io.github.ridiekel.jeletask.mqtt.listener.MqttConnectionSettings;
import io.github.ridiekel.jeletask.mqtt.listener.TestCertificates;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import javax.net.ssl.SSLHandshakeException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * The bridge's TLS settings against a real mosquitto that only accepts TLS with a login, the way a broker that
 * guards a house is set up. The certificate is made for the host on which testcontainers reaches the container,
 * so the name check is real here too.
 */
class End2EndTlsTest {
    private static final String USER = "bridge";
    private static final String PASSWORD = "bridge-secret";

    private static final String CONFIG = """
            listener 8883
            certfile /mosquitto/config/server.pem
            keyfile /mosquitto/config/server.key
            allow_anonymous false
            password_file /mosquitto/config/passwd
            """;

    @TempDir
    static Path directory;

    private static TestCertificates.Issued ca;
    private static GenericContainer<?> broker;

    @BeforeAll
    static void startBroker() {
        ca = TestCertificates.ca("jeletask end2end ca");
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("eclipse-mosquitto:latest"));
        TestCertificates.Issued server = TestCertificates.server(ca, container.getHost(), "localhost", "127.0.0.1");
        broker = container
                .withExposedPorts(8883)
                .withCopyToContainer(Transferable.of(CONFIG), "/mosquitto/config/tls.conf")
                .withCopyToContainer(Transferable.of(server.chainPem()), "/mosquitto/config/server.pem")
                .withCopyToContainer(Transferable.of(server.privateKeyPem(), 0644), "/mosquitto/config/server.key")
                .withCommand("sh", "-c", "mosquitto_passwd -b -c /mosquitto/config/passwd " + USER + " " + PASSWORD
                        + " && chown mosquitto /mosquitto/config/passwd /mosquitto/config/server.key"
                        + " && exec mosquitto -c /mosquitto/config/tls.conf")
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
    void theBridgeConnectsOverTlsAndMessagesGoBothWays() throws Exception {
        MqttConfiguration mqtt = tls(ca.writeCertificate(directory, "ca.pem"));
        MqttConnection bridge = connect(mqtt, USER, PASSWORD, "tls-bridge");
        MqttConnection other = connect(mqtt, USER, PASSWORD, "tls-other");
        try {
            List<String> received = new CopyOnWriteArrayList<>();
            other.subscribe("teletask2mqtt/tls/test", (_, message) -> received.add(message.toString()));
            bridge.publish("teletask2mqtt/tls/test", "over tls".getBytes(), false);

            await("message over TLS").atMost(30, TimeUnit.SECONDS).until(() -> received.contains("over tls"));
        } finally {
            bridge.close();
            other.close();
        }
    }

    @Test
    void aWrongPasswordIsRefusedByTheBroker() throws Exception {
        MqttConfiguration mqtt = tls(ca.writeCertificate(directory, "ca.pem"));
        assertThatThrownBy(() -> connect(mqtt, USER, "wrong", "tls-wrong-password"))
                .isInstanceOf(MqttConnectionException.class)
                .satisfies(e -> assertThat(causedBy(e, Mqtt3ConnAckException.class)).as("refused by the broker in its CONNACK").isTrue());
    }

    @Test
    void aBrokerSignedByAnotherCaIsRefused() throws Exception {
        TestCertificates.Issued stranger = TestCertificates.ca("someone else");
        MqttConfiguration mqtt = tls(stranger.writeCertificate(directory, "stranger.pem"));
        assertThatThrownBy(() -> connect(mqtt, USER, PASSWORD, "tls-stranger"))
                .isInstanceOf(MqttConnectionException.class)
                .satisfies(e -> assertThat(causedBy(e, SSLHandshakeException.class))
                        .as("an SSLHandshakeException in the cause chain").isTrue());
    }

    @Test
    void plainTcpToTheTlsPortDoesNotConnect() {
        MqttConfiguration mqtt = tls(null);
        mqtt.setSsl(false);
        assertThatThrownBy(() -> connect(mqtt, USER, PASSWORD, "tls-plain"))
                .isInstanceOf(MqttConnectionException.class);
    }

    private static MqttConfiguration tls(Path caFile) {
        MqttConfiguration mqtt = new MqttConfiguration();
        mqtt.setHost(broker.getHost());
        mqtt.setPort(String.valueOf(broker.getMappedPort(8883)));
        mqtt.setSsl(true);
        mqtt.setCaFile(caFile == null ? null : caFile.toString());
        return mqtt;
    }

    private static MqttConnection connect(MqttConfiguration mqtt, String user, String password, String clientId) throws MqttConnectionException {
        Duration second = Duration.ofSeconds(1);
        MqttConnection client = new HiveMqConnection();
        client.connect(new MqttConnectionSettings(mqtt, clientId, user, password, Duration.ofSeconds(10), Duration.ofSeconds(10), second, second, Duration.ofSeconds(10), clientId + "/state", "offline"), new MqttConnection.Listener() {
            @Override
            public void connected(boolean reconnect) {
                // Only the outcome of connect matters here
            }

            @Override
            public void connectionLost(Throwable cause) {
                // Only the outcome of connect matters here
            }
        });
        return client;
    }

    private static boolean causedBy(Throwable error, Class<? extends Throwable> type) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }
}
