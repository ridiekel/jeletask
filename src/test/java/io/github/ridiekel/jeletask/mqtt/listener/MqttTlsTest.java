package io.github.ridiekel.jeletask.mqtt.listener;

import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties.MqttConfiguration;
import com.hivemq.client.mqtt.MqttClientSslConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The TLS settings of the bridge, against a small TLS broker in this JVM that answers a CONNECT with an accepting
 * CONNACK. The client of the bridge itself makes the connection, so what is tested is what the bridge does: which certificates it
 * trusts and whether it checks the broker's name. No docker needed; {@code End2EndTlsTest} does the same against
 * a real mosquitto.
 */
class MqttTlsTest {
    @TempDir
    Path directory;

    private FakeTlsBroker broker;

    @AfterEach
    void stopBroker() throws IOException {
        if (broker != null) {
            broker.close();
        }
    }

    // --- broker URL ------------------------------------------------------------------------

    @Test
    void plainTcpByDefault() {
        MqttConfiguration mqtt = config("broker.example", "1883", false, null);
        assertThat(MqttTls.brokerUrl(mqtt)).isEqualTo("tcp://broker.example:1883");
    }

    @Test
    void sslSchemeWhenSslIsOn() {
        MqttConfiguration mqtt = config("broker.example", "8883", true, null);
        assertThat(MqttTls.brokerUrl(mqtt)).isEqualTo("ssl://broker.example:8883");
    }

    // --- options ---------------------------------------------------------------------------

    /**
     * An environment variable that is set but empty, for instance from a compose file with a variable that has no value.
     */
    @Test
    void anEmptyPortIsTheStandardPort() {
        assertThat(MqttTls.brokerUrl(config("broker.local", " ", false, null))).isEqualTo("tcp://broker.local:1883");
        assertThat(MqttTls.brokerUrl(config("broker.local", "", true, null))).isEqualTo("ssl://broker.local:8883");
        assertThat(MqttTls.brokerUrl(config("broker.local", null, false, null))).isEqualTo("tcp://broker.local:1883");
    }

    @Test
    void aPortThatIsNoPortNumberIsAClearError() {
        for (String port : List.of("mqtt", "0", "65536", "-1")) {
            MqttConfiguration mqtt = config("127.0.0.1", port, false, null);

            assertThatThrownBy(() -> connect(mqtt))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("TELETASK_MQTT_PORT is not a port number: '" + port + "'");
        }
    }

    @Test
    void withoutSslThereIsNoTlsConfig() throws IOException {
        Path ca = TestCertificates.ca("unused").writeCertificate(directory, "ca.pem");
        assertThat(MqttTls.sslConfig(config("localhost", "1883", false, ca.toString()))).isEmpty();
    }

    @Test
    void withSslAndNoCaFileTheJvmTrustStoreIsUsed() {
        MqttClientSslConfig ssl = MqttTls.sslConfig(config("localhost", "8883", true, "  ")).orElseThrow();
        assertThat(ssl.getTrustManagerFactory()).isEmpty();
        // No verifier of our own: the client then checks the host name like HTTPS does
        assertThat(ssl.getHostnameVerifier()).isEmpty();
    }

    @Test
    void aCaFileWithoutCertificatesIsRefused() throws IOException {
        Path empty = Files.writeString(directory.resolve("empty.pem"), "");
        assertThatThrownBy(() -> MqttTls.trustManagers(empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No certificate");
    }

    @Test
    void anUnreadableCaFileIsAClearError() {
        Path missing = directory.resolve("missing.pem");
        assertThatThrownBy(() -> MqttTls.trustManagers(missing))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(missing.toString());
    }

    // --- against a TLS broker ----------------------------------------------------------------

    @Test
    void connectsWhenTheCaIsTrustedAndTheNameMatches() throws Exception {
        TestCertificates.Issued ca = TestCertificates.ca("jeletask test ca");
        broker = new FakeTlsBroker(TestCertificates.server(ca, "localhost", "127.0.0.1"));
        MqttConfiguration mqtt = config("localhost", broker.port(), true, ca.writeCertificate(directory, "ca.pem").toString());

        MqttConnection client = connect(mqtt);
        try {
            assertThat(client.isConnected()).isTrue();
            assertThat(broker.handshakes()).isEqualTo(1);
        } finally {
            client.close();
        }
    }

    @Test
    void refusesABrokerSignedByAnotherCa() throws Exception {
        TestCertificates.Issued ca = TestCertificates.ca("jeletask test ca");
        TestCertificates.Issued stranger = TestCertificates.ca("someone else");
        broker = new FakeTlsBroker(TestCertificates.server(stranger, "localhost", "127.0.0.1"));
        MqttConfiguration mqtt = config("localhost", broker.port(), true, ca.writeCertificate(directory, "ca.pem").toString());

        assertRefusedDuringHandshake(mqtt);
    }

    @Test
    void refusesABrokerWhoseNameDoesNotMatch() throws Exception {
        TestCertificates.Issued ca = TestCertificates.ca("jeletask test ca");
        broker = new FakeTlsBroker(TestCertificates.server(ca, "mqtt.example.invalid"));
        MqttConfiguration mqtt = config("localhost", broker.port(), true, ca.writeCertificate(directory, "ca.pem").toString());

        assertRefusedDuringHandshake(mqtt);
    }

    @Test
    void withoutACaFileAPrivateCaIsNotTrusted() throws Exception {
        // Proves that "no CA file" means the JVM trust store, and not "trust anything".
        TestCertificates.Issued ca = TestCertificates.ca("jeletask test ca");
        broker = new FakeTlsBroker(TestCertificates.server(ca, "localhost", "127.0.0.1"));

        assertRefusedDuringHandshake(config("localhost", broker.port(), true, null));
    }

    @Test
    void plainTcpToATlsBrokerDoesNotConnect() throws Exception {
        TestCertificates.Issued ca = TestCertificates.ca("jeletask test ca");
        broker = new FakeTlsBroker(TestCertificates.server(ca, "localhost", "127.0.0.1"));

        assertThatThrownBy(() -> connect(config("localhost", broker.port(), false, null)))
                .isInstanceOf(MqttConnectionException.class);
        assertThat(broker.handshakes()).isZero();
    }

    // --- helpers -----------------------------------------------------------------------------

    private void assertRefusedDuringHandshake(MqttConfiguration mqtt) {
        assertThatThrownBy(() -> connect(mqtt))
                .isInstanceOf(MqttConnectionException.class)
                .satisfies(e -> assertThat(hasCause(e, SSLHandshakeException.class))
                        .as("an SSLHandshakeException in the cause chain of %s", chain(e)).isTrue());
        assertThat(broker.handshakes()).isZero();
    }

    private static String chain(Throwable error) {
        StringBuilder chain = new StringBuilder();
        for (Throwable t = error; t != null; t = t.getCause()) {
            chain.append(" <- ").append(t);
        }
        return chain.toString();
    }

    private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    private static MqttConnection connect(MqttConfiguration mqtt) throws MqttConnectionException {
        MqttConnection client = new HiveMqConnection();
        client.connect(settings(mqtt, "mqtt-tls-test"), new MqttConnection.Listener() {
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

    static MqttConnectionSettings settings(MqttConfiguration mqtt, String clientId) {
        Duration second = Duration.ofSeconds(1);
        return new MqttConnectionSettings(mqtt, clientId, null, null, Duration.ofSeconds(10), Duration.ofSeconds(5), second, second, Duration.ofSeconds(10), clientId + "/state", "offline");
    }

    private static MqttConfiguration config(String host, String port, boolean ssl, String caFile) {
        MqttConfiguration mqtt = new MqttConfiguration();
        mqtt.setHost(host);
        mqtt.setPort(port);
        mqtt.setSsl(ssl);
        mqtt.setCaFile(caFile);
        return mqtt;
    }

    /**
     * Accepts TLS connections and answers every MQTT CONNECT with "connection accepted". Counts the handshakes that
     * completed, so a test can tell "refused during the handshake" from "refused later".
     */
    private static final class FakeTlsBroker implements AutoCloseable {
        private final SSLServerSocket server;
        private final AtomicInteger handshakes = new AtomicInteger();

        FakeTlsBroker(TestCertificates.Issued certificate) throws Exception {
            KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keys.init(certificate.keyStore(), certificate.keyStorePassword());
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keys.getKeyManagers(), null, null);
            server = (SSLServerSocket) context.getServerSocketFactory().createServerSocket(0, 5, InetAddress.getLoopbackAddress());
            Thread.ofVirtual().start(this::serve);
        }

        String port() {
            return String.valueOf(server.getLocalPort());
        }

        int handshakes() {
            return handshakes.get();
        }

        private void serve() {
            while (!server.isClosed()) {
                try {
                    Socket socket = server.accept();
                    Thread.ofVirtual().start(() -> answer((SSLSocket) socket));
                } catch (IOException _) {
                    return;
                }
            }
        }

        private void answer(SSLSocket socket) {
            try (socket) {
                socket.startHandshake();
                handshakes.incrementAndGet();
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                while (true) {
                    int header = in.read();
                    if (header < 0) {
                        return;
                    }
                    in.skipNBytes(remainingLength(in));
                    if ((header & 0xF0) == 0x10) {
                        // CONNECT -> CONNACK, session not present, accepted
                        out.write(new byte[]{0x20, 0x02, 0x00, 0x00});
                        out.flush();
                    }
                }
            } catch (IOException _) {
                // A refused handshake or a closed connection: both end this exchange.
            }
        }

        private static int remainingLength(InputStream in) throws IOException {
            int value = 0;
            int multiplier = 1;
            int digit;
            do {
                digit = in.read();
                if (digit < 0) {
                    throw new IOException("closed");
                }
                value += (digit & 0x7F) * multiplier;
                multiplier *= 128;
            } while ((digit & 0x80) != 0);
            return value;
        }

        @Override
        public void close() throws IOException {
            server.close();
        }
    }
}
