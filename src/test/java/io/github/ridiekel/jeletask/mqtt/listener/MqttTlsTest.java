package io.github.ridiekel.jeletask.mqtt.listener;

import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties.MqttConfiguration;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The TLS settings of the bridge, against a small TLS broker in this JVM that answers a CONNECT with an accepting
 * CONNACK. Paho itself makes the connection, so what is tested is what the bridge does: which certificates it
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

    @Test
    void withoutSslTheOptionsAreLeftAlone() throws IOException {
        Path ca = TestCertificates.ca("unused").writeCertificate(directory, "ca.pem");
        MqttConnectOptions options = new MqttConnectOptions();
        MqttTls.apply(config("localhost", "1883", false, ca.toString()), options);
        assertThat(options.getSocketFactory()).isNull();
    }

    @Test
    void withSslAndNoCaFileTheJvmTrustStoreIsUsed() {
        MqttConnectOptions options = new MqttConnectOptions();
        MqttTls.apply(config("localhost", "8883", true, "  "), options);
        assertThat(options.getSocketFactory()).isNull();
        assertThat(options.isHttpsHostnameVerificationEnabled()).isTrue();
    }

    @Test
    void aCaFileWithoutCertificatesIsRefused() throws IOException {
        Path empty = Files.writeString(directory.resolve("empty.pem"), "");
        assertThatThrownBy(() -> MqttTls.socketFactory(empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No certificate");
    }

    @Test
    void anUnreadableCaFileIsAClearError() {
        Path missing = directory.resolve("missing.pem");
        assertThatThrownBy(() -> MqttTls.socketFactory(missing))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(missing.toString());
    }

    // --- against a TLS broker ----------------------------------------------------------------

    @Test
    void connectsWhenTheCaIsTrustedAndTheNameMatches() throws Exception {
        TestCertificates.Issued ca = TestCertificates.ca("jeletask test ca");
        broker = new FakeTlsBroker(TestCertificates.server(ca, "localhost", "127.0.0.1"));
        MqttConfiguration mqtt = config("localhost", broker.port(), true, ca.writeCertificate(directory, "ca.pem").toString());

        MqttClient client = connect(mqtt);
        try {
            assertThat(client.isConnected()).isTrue();
            assertThat(broker.handshakes()).isEqualTo(1);
        } finally {
            client.disconnectForcibly(0, 0, false);
            client.close(true);
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
                .isInstanceOf(MqttException.class);
        assertThat(broker.handshakes()).isZero();
    }

    // --- helpers -----------------------------------------------------------------------------

    private void assertRefusedDuringHandshake(MqttConfiguration mqtt) {
        assertThatThrownBy(() -> connect(mqtt))
                .isInstanceOf(MqttException.class)
                .satisfies(e -> assertThat(hasCause(e, SSLHandshakeException.class))
                        .as("an SSLHandshakeException in the cause chain").isTrue());
        assertThat(broker.handshakes()).isZero();
    }

    private static boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    private static MqttClient connect(MqttConfiguration mqtt) throws MqttException {
        MqttClient client = new MqttClient(MqttTls.brokerUrl(mqtt), "mqtt-tls-test", new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setConnectionTimeout(5);
        MqttTls.apply(mqtt, options);
        try {
            client.connect(options);
        } catch (MqttException e) {
            client.close(true);
            throw e;
        }
        return client;
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
                } catch (IOException e) {
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
            } catch (IOException e) {
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
