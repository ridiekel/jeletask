package io.github.ridiekel.jeletask.mqtt.listener;

import io.github.ridiekel.jeletask.HealthCheck;
import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.mqtt.container.mqtt.MqttContainer;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessorFixtures.FakeTeletaskClient;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.github.ridiekel.jeletask.mqtt.listener.MqttProcessorFixtures.BASE_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
/**
 * The real MQTT client against a real broker that restarts and loses all its sessions and retained messages: mosquitto-no-auth.conf
 * has no persistence. Skipped without Docker, and not an End2End test: no Home Assistant or browser involved.
 * <p>
 * Only the broker process restarts, not the container: a restarted container may get another host port, and the
 * client reconnects to the same address. A fixed host port does not help, not every Docker host forwards every port.
 */
@EnabledIf("dockerAvailable")
class MqttProcessorBrokerReconnectTest {
    private static final Duration AWAIT = Duration.ofSeconds(60);
    // While this file exists the broker stays down
    private static final String HOLD = "/tmp/broker-down";

    private final FakeTeletaskClient teletask = new FakeTeletaskClient();
    private final Teletask2MqttConfigurationProperties configuration = MqttProcessorFixtures.configuration();
    private final CentralUnit centralUnit = MqttProcessorFixtures.centralUnit();
    private final MqttProcessor processor = new MqttProcessor(centralUnit, teletask, configuration, _ -> {
    }, () -> bridge = new HiveMqConnection());
    private final MqttContainer broker = new MqttContainer(configuration, processor, centralUnit);
    private final List<String> availability = new CopyOnWriteArrayList<>();
    private MqttConnection bridge;
    private MqttClient observer;

    static boolean dockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    @AfterEach
    void tearDown() throws MqttException {
        try {
            closeObserver();
            // They reconnect automatically, and a later broker can get the same port
            if (bridge != null) {
                bridge.close();
            }
            broker.closeCaptureClient();
        } finally {
            broker.stop();
        }
    }

    @Test
    void afterABrokerRestartTheBridgeSubscribesAgainAndPublishesOnline() throws Exception {
        broker.withCommand("sh", "-c", "while true; do [ -f " + HOLD + " ] || mosquitto -c /mosquitto-no-auth.conf; sleep 0.2; done");
        broker.start();
        processor.startup();
        processor.publishConnectionStatus();
        connectObserver();
        awaitAvailabilityOnline();

        closeObserver();
        availability.clear();

        inBroker("touch " + HOLD + " && pkill mosquitto");
        Awaitility.await("bridge notices the broker is gone").atMost(AWAIT).until(() -> health().contains("mqtt=DOWN"));

        inBroker("rm " + HOLD);
        connectObserver();

        // The broker forgot its retained messages, so only the reconnect can bring this back
        awaitAvailabilityOnline();

        // And forgot the subscriptions of the bridge
        int groupGets = teletask.groupGets.get();
        observer.publish(BASE_TOPIC + "/refresh", "after restart".getBytes(), 1, false);
        Awaitility.await("refresh after reconnect").atMost(AWAIT).until(() -> teletask.groupGets.get() > groupGets);
    }

    private void closeObserver() throws MqttException {
        close(observer);
        observer = null;
    }

    private static void close(MqttClient client) throws MqttException {
        if (client != null) {
            client.disconnectForcibly(0, 0, false);
            client.close(true);
        }
    }

    private void connectObserver() throws MqttException {
        observer = new MqttClient("tcp://" + broker.getHost() + ":" + broker.getPort(), "reconnect-test-observer", new MemoryPersistence());
        Awaitility.await("observer connected").atMost(AWAIT).ignoreExceptions().until(() -> {
            observer.connect();
            return observer.isConnected();
        });
        observer.subscribe(processor.getAvailabilityTopic(), 1, (_, message) -> availability.add(new String(message.getPayload())));
    }

    private void awaitAvailabilityOnline() {
        Awaitility.await("availability online").atMost(AWAIT)
                .until(() -> !availability.isEmpty() && Objects.equals(availability.getLast(), "online"));
    }

    private String health() {
        processor.checkAndPublishConnectedStatus();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HealthCheck.getDefault().check(new PrintStream(out));
        return out.toString();
    }

    private void inBroker(String command) throws IOException, InterruptedException {
        Container.ExecResult result = broker.execInContainer("sh", "-c", command);
        assertThat(result.getExitCode()).as(command + ": " + result.getStderr()).isZero();
    }
}
