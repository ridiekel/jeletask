package io.github.ridiekel.jeletask.mqtt.listener;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.toxiproxy.ToxiproxyContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;

/**
 * Toxiproxy and mosquitto on one Docker network. The central unit lives in the test JVM, Toxiproxy reaches it through
 * host.testcontainers.internal.
 */
final class ResilienceContainers implements ResilienceNetwork {
    private static final int CENTRAL_LISTEN_PORT = 8666;
    private static final int BROKER_LISTEN_PORT = 8667;
    private static final int BROKER_PORT = 1883;
    private static final String BROKER_ALIAS = "mqtt";
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(5);

    private final Network network = Network.newNetwork();
    private final GenericContainer<?> broker = new GenericContainer<>(DockerImageName.parse("eclipse-mosquitto:2.0.22"))
            .withNetwork(this.network)
            .withNetworkAliases(BROKER_ALIAS)
            .withExposedPorts(BROKER_PORT)
            .withCommand("mosquitto -c /mosquitto-no-auth.conf")
            .withStartupTimeout(STARTUP_TIMEOUT);
    private final ToxiproxyContainer toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.13.0")
            .withNetwork(this.network)
            .withStartupTimeout(STARTUP_TIMEOUT);
    private Proxy centralLine;
    private Proxy brokerLine;

    static ResilienceContainers start(int centralPort) throws IOException {
        ResilienceContainers containers = new ResilienceContainers();
        try {
            containers.startAll(centralPort);
            return containers;
        } catch (IOException | RuntimeException e) {
            containers.close();
            throw e;
        }
    }

    private void startAll(int centralPort) throws IOException {
        // Before Toxiproxy starts: the hostname only ends up in containers started after the first exposed port
        Testcontainers.exposeHostPorts(centralPort);
        this.broker.start();
        this.toxiproxy.start();

        ToxiproxyClient client = new ToxiproxyClient(this.toxiproxy.getHost(), this.toxiproxy.getControlPort());
        this.centralLine = client.createProxy("central", "0.0.0.0:" + CENTRAL_LISTEN_PORT, GenericContainer.INTERNAL_HOST_HOSTNAME + ":" + centralPort);
        this.brokerLine = client.createProxy("broker", "0.0.0.0:" + BROKER_LISTEN_PORT, BROKER_ALIAS + ":" + BROKER_PORT);
    }

    @Override
    public String host() {
        return this.toxiproxy.getHost();
    }

    @Override
    public int proxiedCentralPort() {
        return this.toxiproxy.getMappedPort(CENTRAL_LISTEN_PORT);
    }

    @Override
    public int proxiedBrokerPort() {
        return this.toxiproxy.getMappedPort(BROKER_LISTEN_PORT);
    }

    @Override
    public Proxy centralLine() {
        return this.centralLine;
    }

    @Override
    public Proxy brokerLine() {
        return this.brokerLine;
    }

    @Override
    public String observerBrokerUrl() {
        return "tcp://" + this.broker.getHost() + ":" + this.broker.getMappedPort(BROKER_PORT);
    }

    @Override
    public void close() {
        this.toxiproxy.stop();
        this.broker.stop();
        this.network.close();
    }
}
