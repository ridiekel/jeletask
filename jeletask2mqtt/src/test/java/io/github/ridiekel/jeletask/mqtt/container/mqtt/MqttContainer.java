package io.github.ridiekel.jeletask.mqtt.container.mqtt;

import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.mqtt.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.mqtt.container.ha.HomeAssistantContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Service
public class MqttContainer extends GenericContainer<MqttContainer> {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Teletask2MqttConfigurationProperties properties;
    private MqttClient mqttClient;

    private final List<MqttCapture> messages = new ArrayList<>();

    public MqttContainer(Teletask2MqttConfigurationProperties properties) {
        super(DockerImageName.parse("eclipse-mosquitto:latest"));
        this.properties = properties;
        this.withExposedPorts(1883)
                .withNetworkAliases("mqtt")
                .withCommand("mosquitto -c /mosquitto-no-auth.conf")
                .withNetwork(Network.newNetwork());
    }

    @EventListener(classes = {ContextRefreshedEvent.class})
    @Order(100)
    public void start() {
        LOGGER.info(AnsiOutput.toString(AnsiColor.BRIGHT_MAGENTA, "Starting MQTT", AnsiColor.DEFAULT));

        super.start();
        this.properties.getMqtt().setPort(String.valueOf(getPort()));

        String broker = "tcp://localhost:" + this.getPort();

        try {
            mqttClient = new MqttClient(broker, UUID.randomUUID().toString(), new MemoryPersistence());
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }

        Awaitility.await("MQTT Test Client Connected").atMost(1, TimeUnit.MINUTES).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            boolean connected = false;
            try {
                mqttClient.connect();
                connected = mqttClient.isConnected();
                System.out.println();
            } catch (MqttException e) {
                connected = false;
            }
            return connected;
        });

        LOGGER.info(AnsiOutput.toString(AnsiColor.BRIGHT_GREEN, "MQTT started and test client connected", AnsiColor.DEFAULT));
    }

    public void startCapturing() {
        this.subscribe("test_prefix_teletask2mqtt/MAN_TEST_localhost_1234/#", (t, m) -> {
            MqttCapture capture = new MqttCapture(t, new String(m.getPayload()));
            LOGGER.info(AnsiOutput.toString("Captured '", AnsiColor.BRIGHT_CYAN, t, AnsiColor.DEFAULT, "' - ", AnsiColor.BRIGHT_YELLOW, capture.getMessage(), AnsiColor.DEFAULT));
            this.messages.add(capture);
        });
    }

    public void subscribe(String topicFilter, IMqttMessageListener messageListener) {
        try {
            this.mqttClient.subscribe(topicFilter, messageListener);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    public Integer getPort() {
        return this.getFirstMappedPort();
    }

    public void reset() {
        LOGGER.info(AnsiOutput.toString(AnsiColor.BLUE, "Resetting captured messages", AnsiColor.DEFAULT));
        this.messages.clear();
    }

    public TopicExpectationBuilder expectLastStateMessage(Function function, int number) {
        String topic = "test_prefix_teletask2mqtt/MAN_TEST_localhost_1234/" + function.toString().toLowerCase() + "/" + number + "/state";
        return new TopicExpectationBuilder(() -> this.messages.stream()
                .sorted(Comparator.comparing(MqttCapture::getTimestamp).reversed())
                .filter(m -> Objects.equals(m.getTopic(), topic))
                .findFirst(), topic);
    }

    public static class TopicExpectationBuilder {
        private final String topic;
        private final Supplier<Optional<MqttCapture>> message;

        public TopicExpectationBuilder(Supplier<Optional<MqttCapture>> message, String topic) {
            this.topic = topic;
            this.message = message;
        }

        public void toHaveState(String state) {
            this.toMatch("state: '" + state + "'", m -> Objects.equals(m.asState().getState(), state));
        }

        public void toMatch(String describe, Predicate<MqttCapture> matcher) {
            String msg = AnsiOutput.toString("[%s]", AnsiColor.DEFAULT, " Expectation for topic '", AnsiColor.BRIGHT_CYAN, topic, AnsiColor.DEFAULT, "': ", AnsiColor.BRIGHT_YELLOW, describe, AnsiColor.DEFAULT);

            Awaitility.await(AnsiOutput.toString(AnsiColor.BRIGHT_RED, String.format(msg, "FAILED")))
                    .pollInterval(250, TimeUnit.MILLISECONDS)
                    .atMost(10, TimeUnit.SECONDS)
                    .until(() -> this.message.get().map(matcher::test).orElse(false));

            LOGGER.info(AnsiOutput.toString(AnsiColor.BRIGHT_GREEN, String.format(msg, "SUCCESS")));
        }
    }

    public static class MqttCapture {
        private final LocalDateTime timestamp;
        private final String topic;
        private final String message;

        public MqttCapture(String topic, String message) {
            this.topic = topic;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getTopic() {
            return topic;
        }

        public String getMessage() {
            return message;
        }

        public ComponentState asState() {
            try {
                return HomeAssistantContainer.OBJECT_MAPPER.readValue(this.getMessage(), ComponentState.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
