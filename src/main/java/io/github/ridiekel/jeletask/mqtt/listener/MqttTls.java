package io.github.ridiekel.jeletask.mqtt.listener;

import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties.MqttConfiguration;
import com.hivemq.client.mqtt.MqttClientSslConfig;

import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.Optional;

/**
 * How the bridge reaches the broker: plain tcp:// or TLS (ssl://), and which certificates it trusts for TLS.
 */
public final class MqttTls {
    private MqttTls() {
    }

    public static String brokerUrl(MqttConfiguration mqtt) {
        return (mqtt.isSsl() ? "ssl://" : "tcp://") + mqtt.getHost() + ":" + mqtt.getPort();
    }

    /**
     * The TLS settings, absent without ssl.
     * <p>
     * The host name of the broker is always checked against its certificate. Without a CA file the client uses the
     * trust store of the JVM; with one, only the certificates in that file are trusted.
     */
    public static Optional<MqttClientSslConfig> sslConfig(MqttConfiguration mqtt) {
        if (!mqtt.isSsl()) {
            return Optional.empty();
        }
        // Without a hostname verifier the client checks the host name itself, like HTTPS does
        return Optional.of(caFile(mqtt)
                .map(file -> MqttClientSslConfig.builder().trustManagerFactory(trustManagers(file)).build())
                .orElseGet(() -> MqttClientSslConfig.builder().build()));
    }

    static Optional<Path> caFile(MqttConfiguration mqtt) {
        return Optional.ofNullable(mqtt.getCaFile()).map(String::trim).filter(f -> !f.isEmpty()).map(Path::of);
    }

    static TrustManagerFactory trustManagers(Path caFile) {
        try (InputStream in = Files.newInputStream(caFile)) {
            Collection<? extends Certificate> certificates = CertificateFactory.getInstance("X.509").generateCertificates(in);
            if (certificates.isEmpty()) {
                throw new IllegalArgumentException("No certificate in MQTT CA file " + caFile);
            }
            KeyStore trusted = KeyStore.getInstance(KeyStore.getDefaultType());
            trusted.load(null, null);
            int index = 0;
            for (Certificate certificate : certificates) {
                trusted.setCertificateEntry("ca-" + index++, certificate);
            }
            TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagers.init(trusted);
            return trustManagers;
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Could not read MQTT CA file " + caFile + ": " + e.getMessage(), e);
        }
    }
}
