package io.github.ridiekel.jeletask.end2end;

import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties.MqttConfiguration;
import io.github.ridiekel.jeletask.mqtt.listener.MqttTls;
import io.github.ridiekel.jeletask.mqtt.listener.TestCertificates;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
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
        MqttClient bridge = connect(mqtt, USER, PASSWORD, "tls-bridge");
        MqttClient other = connect(mqtt, USER, PASSWORD, "tls-other");
        try {
            List<String> received = new CopyOnWriteArrayList<>();
            other.subscribe("teletask2mqtt/tls/test", 1, (_, message) -> received.add(new String(message.getPayload())));
            bridge.publish("teletask2mqtt/tls/test", "over tls".getBytes(), 1, false);

            await("message over TLS").atMost(30, TimeUnit.SECONDS).until(() -> received.contains("over tls"));
        } finally {
            close(bridge);
            close(other);
        }
    }

    @Test
    void aWrongPasswordIsRefusedByTheBroker() throws Exception {
        MqttConfiguration mqtt = tls(ca.writeCertificate(directory, "ca.pem"));
        assertThatThrownBy(() -> connect(mqtt, USER, "wrong", "tls-wrong-password"))
                .isInstanceOf(MqttSecurityException.class);
    }

    @Test
    void aBrokerSignedByAnotherCaIsRefused() throws Exception {
        TestCertificates.Issued stranger = TestCertificates.ca("someone else");
        MqttConfiguration mqtt = tls(stranger.writeCertificate(directory, "stranger.pem"));
        assertThatThrownBy(() -> connect(mqtt, USER, PASSWORD, "tls-stranger"))
                .isInstanceOf(MqttException.class)
                .satisfies(e -> assertThat(causedBy(e, SSLHandshakeException.class))
                        .as("an SSLHandshakeException in the cause chain").isTrue());
    }

    @Test
    void plainTcpToTheTlsPortDoesNotConnect() {
        MqttConfiguration mqtt = tls(null);
        mqtt.setSsl(false);
        assertThatThrownBy(() -> connect(mqtt, USER, PASSWORD, "tls-plain"))
                .isInstanceOf(MqttException.class);
    }

    private static MqttConfiguration tls(Path caFile) {
        MqttConfiguration mqtt = new MqttConfiguration();
        mqtt.setHost(broker.getHost());
        mqtt.setPort(String.valueOf(broker.getMappedPort(8883)));
        mqtt.setSsl(true);
        mqtt.setCaFile(caFile == null ? null : caFile.toString());
        return mqtt;
    }

    private static MqttClient connect(MqttConfiguration mqtt, String user, String password, String clientId) throws MqttException {
        MqttClient client = new MqttClient(MqttTls.brokerUrl(mqtt), clientId, new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(user);
        options.setPassword(password.toCharArray());
        options.setConnectionTimeout(10);
        MqttTls.apply(mqtt, options);
        try {
            client.connect(options);
        } catch (MqttException e) {
            client.close(true);
            throw e;
        }
        return client;
    }

    private static void close(MqttClient client) throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
        }
        client.close(true);
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
