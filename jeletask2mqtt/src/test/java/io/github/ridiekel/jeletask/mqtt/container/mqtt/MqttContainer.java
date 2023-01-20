package io.github.ridiekel.jeletask.mqtt.container.mqtt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.mqtt.Teletask2MqttConfigurationProperties;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.github.ridiekel.jeletask.mqtt.container.ha.HomeAssistantContainer.OBJECT_MAPPER;

@Service
public class MqttContainer extends GenericContainer<MqttContainer> {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Teletask2MqttConfigurationProperties properties;
    private MqttClient mqttClient;

    private final List<MqttCapture> captures = new ArrayList<>();

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
            String message = new String(m.getPayload());
            LOGGER.info(AnsiOutput.toString("Captured '", AnsiColor.BRIGHT_CYAN, t, AnsiColor.DEFAULT, "' - ", AnsiColor.BRIGHT_YELLOW, message, AnsiColor.DEFAULT));
            this.captures.add(new MqttCapture(t, message));
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
        this.captures.clear();
    }

    public MqttExpectationBuilder expect() {
        return new MqttExpectationBuilder(captures);
    }

    public static class MqttExpectationBuilder {
        private final List<MqttCapture> captures;

        public MqttExpectationBuilder(List<MqttCapture> captures) {
            this.captures = captures;
        }

        public TopicExpectationBuilder lastStateMessage(Function function, int number) {
            String topic = "test_prefix_teletask2mqtt/MAN_TEST_localhost_1234/" + function.toString().toLowerCase() + "/" + number + "/state";
            return new TopicExpectationBuilder(() -> this.captures.stream()
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

            public MessageExpectationBuilder toHave() {
                return new MessageExpectationBuilder(this);
            }

            public static class MessageExpectationBuilder {
                private final TopicExpectationBuilder topicBuilder;

                public MessageExpectationBuilder(TopicExpectationBuilder topicBuilder) {
                    this.topicBuilder = topicBuilder;
                }

                public void state(String state) {
                    this.match("state: '" + state + "'", m -> Objects.equals(m.getComponentState().getState(), state));
                }

                public void match(String describe, Predicate<MqttCapture> matcher) {
                    String msg = AnsiOutput.toString("[%s]", AnsiColor.DEFAULT, " Expectation for topic '", AnsiColor.BRIGHT_CYAN, topicBuilder.topic, AnsiColor.DEFAULT, "': ", AnsiColor.BRIGHT_YELLOW, describe, AnsiColor.DEFAULT);

                    AtomicReference<MqttCapture> capture = new AtomicReference<>();
                    try {
                        Awaitility.await(describe)
                                .pollInterval(250, TimeUnit.MILLISECONDS)
                                .atMost(10, TimeUnit.SECONDS)
                                .until(() -> {
                                    Optional<MqttCapture> mqttCapture = topicBuilder.message.get();
                                    capture.set(mqttCapture.orElse(null));
                                    return mqttCapture.map(matcher::test).orElse(false);
                                });
                    } catch (Exception e) {
                        LOGGER.error(AnsiOutput.toString(AnsiColor.BRIGHT_RED, String.format(msg, "FAILED"), " - was: ", AnsiColor.RED, capture.get(), AnsiColor.DEFAULT));
                        throw e;
                    }

                    LOGGER.info(AnsiOutput.toString(AnsiColor.BRIGHT_GREEN, String.format(msg, "SUCCESS")));
                }
            }
        }
    }

    public static class MqttCapture {
        private final LocalDateTime timestamp;
        private final String topic;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private final String message;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private final ComponentState componentState;

        public MqttCapture(String topic, String message) {
            this.topic = topic;
            this.componentState = toComponentState(message);
            this.message = this.componentState == null ? message : null;
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

        public ComponentState getComponentState() {
            return componentState;
        }

        private static ComponentState toComponentState(String message) {
            ComponentState state;
            try {
                state = ComponentState.OBJECT_MAPPER.readValue(message, ComponentState.class);
            } catch (IOException e) {
                state = null;
            }
            return state;
        }

        @Override
        public String toString() {
            try {
                return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
