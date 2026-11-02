package io.github.ridiekel.jeletask.mqtt.listener;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Certificates for the TLS tests, made at test time: a CA, and a server certificate signed by it for exactly the
 * names a test needs. Made here rather than kept in the repository, because the end-to-end test only learns at run
 * time on which host the broker container answers.
 * <p>
 * Only Bouncy Castle's certificate builder is used; keys and signatures come from the JDK's own providers.
 */
public final class TestCertificates {
    private static final AtomicLong SERIAL = new AtomicLong(System.currentTimeMillis());
    private static final Pattern IP = Pattern.compile("^[0-9.]+$|^[0-9a-fA-F:]+:[0-9a-fA-F:]*$");
    private static final char[] PASSWORD = "test".toCharArray();

    private TestCertificates() {
    }

    public record Issued(KeyPair keys, X509Certificate certificate, X509Certificate issuer) {
        public String certificatePem() {
            return pem("CERTIFICATE", encoded(certificate));
        }

        /** The certificate followed by its issuer, as a server presents it. */
        public String chainPem() {
            return issuer == null ? certificatePem() : certificatePem() + pem("CERTIFICATE", encoded(issuer));
        }

        /** PKCS#8, which both the JDK and OpenSSL (mosquitto) read. */
        public String privateKeyPem() {
            return pem("PRIVATE KEY", keys.getPrivate().getEncoded());
        }

        public KeyStore keyStore() {
            try {
                KeyStore store = KeyStore.getInstance("PKCS12");
                store.load(null, null);
                X509Certificate[] chain = issuer == null
                        ? new X509Certificate[]{certificate}
                        : new X509Certificate[]{certificate, issuer};
                store.setKeyEntry("server", keys.getPrivate(), PASSWORD, chain);
                return store;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        public char[] keyStorePassword() {
            return PASSWORD.clone();
        }

        public Path writeCertificate(Path directory, String name) throws IOException {
            return Files.writeString(directory.resolve(name), certificatePem(), StandardCharsets.US_ASCII);
        }
    }

    public static Issued ca(String commonName) {
        KeyPair keys = rsa();
        X500Name subject = new X500Name("CN=" + commonName);
        return new Issued(keys, sign(subject, subject, keys, keys, true), null);
    }

    public static Issued server(Issued ca, String... names) {
        KeyPair keys = rsa();
        X500Name subject = new X500Name("CN=" + names[0]);
        X500Name issuer = new X500Name(ca.certificate().getSubjectX500Principal().getName());
        return new Issued(keys, sign(subject, issuer, keys, ca.keys(), false, names), ca.certificate());
    }

    private static X509Certificate sign(X500Name subject, X500Name issuer, KeyPair subjectKeys, KeyPair issuerKeys,
                                        boolean isCa, String... names) {
        try {
            Instant now = Instant.now();
            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuer,
                    BigInteger.valueOf(SERIAL.incrementAndGet()), Date.from(now.minus(Duration.ofHours(1))),
                    Date.from(now.plus(Duration.ofDays(2))), subject, subjectKeys.getPublic());
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(isCa));
            if (names.length > 0) {
                GeneralName[] alternatives = new GeneralName[names.length];
                for (int i = 0; i < names.length; i++) {
                    alternatives[i] = new GeneralName(IP.matcher(names[i]).matches() ? GeneralName.iPAddress : GeneralName.dNSName, names[i]);
                }
                builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(alternatives));
            }
            X509CertificateHolder holder = builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(issuerKeys.getPrivate()));
            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(holder.getEncoded()));
        } catch (Exception e) {
            throw new IllegalStateException("Could not make a test certificate", e);
        }
    }

    private static KeyPair rsa() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] encoded(X509Certificate certificate) {
        try {
            return certificate.getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String pem(String type, byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
    }
}
