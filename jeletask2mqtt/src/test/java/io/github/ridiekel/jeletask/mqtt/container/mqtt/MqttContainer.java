package io.github.ridiekel.jeletask.mqtt.container.mqtt;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.mqtt.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static io.github.ridiekel.jeletask.mqtt.container.ha.HomeAssistantContainer.OBJECT_MAPPER;

@Service
public class MqttContainer extends GenericContainer<MqttContainer> {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Teletask2MqttConfigurationProperties properties;
    private final MqttProcessor mqttProcessor;
    private MqttClient mqttClient;

    private final List<MqttCapture> captures = new ArrayList<>();

    public MqttContainer(Teletask2MqttConfigurationProperties properties, MqttProcessor mqttProcessor) {
        super(DockerImageName.parse("eclipse-mosquitto:latest"));
        this.properties = properties;
        this.mqttProcessor = mqttProcessor;
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
            mqttClient = new MqttClient(broker, this.getClass().getSimpleName() + "-" + UUID.randomUUID().toString(), new MemoryPersistence());
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }

        Awaitility.await("MQTT Test Client Connected").atMost(1, TimeUnit.MINUTES).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            boolean connected = false;
            try {
                mqttClient.connect();
                connected = mqttClient.isConnected();
            } catch (MqttException e) {
                connected = false;
            }
            return connected;
        });

        this.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger(this.getClass())).withPrefix(AnsiOutput.toString(AnsiColor.BLUE, "mqtt-container-log", AnsiColor.DEFAULT)));

        LOGGER.info(AnsiOutput.toString(AnsiColor.BRIGHT_GREEN, "MQTT started and test client connected ", AnsiColor.BRIGHT_WHITE, "(port: ", this.getPort(), ")", AnsiColor.DEFAULT));
    }

    public MqttProcessor processor() {
        return mqttProcessor;
    }

    public void startCapturing() {
        this.subscribe("#", (t, m) -> {
            String message = new String(m.getPayload());
            LOGGER.info(AnsiOutput.toString("Captured '", AnsiColor.BRIGHT_CYAN, t, AnsiColor.DEFAULT, "' - ", AnsiColor.BRIGHT_YELLOW, message, AnsiColor.DEFAULT, " - replay using:\n\t\t", AnsiColor.BRIGHT_WHITE, "mosquitto_pub -h localhost -p ", this.getPort(), " -t '", t, "' -m '", m, "'", AnsiColor.DEFAULT));
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
        return new MqttExpectationBuilder(this);
    }

    public static class MqttExpectationBuilder {
        private final MqttContainer mqttContainer;

        public MqttExpectationBuilder(MqttContainer mqttContainer) {
            this.mqttContainer = mqttContainer;
        }

        public MqttFunctionExpectationBuilder relay(int number) {
            return function(Function.RELAY, number);
        }

        public MqttFunctionExpectationBuilder function(Function function, int number) {
            return new MqttFunctionExpectationBuilder(this, function, number);
        }

        public static class MqttFunctionExpectationBuilder {
            private final Function function;
            private final int number;
            private final MqttExpectationBuilder mqttBuilder;

            public MqttFunctionExpectationBuilder(MqttExpectationBuilder mqttBuilder, Function function, int number) {
                this.mqttBuilder = mqttBuilder;
                this.function = function;
                this.number = number;
            }

            public TopicExpectationBuilder lastStateMessage() {
                return new TopicExpectationBuilder(this, "state");
            }

            public TopicExpectationBuilder lastSetMessage() {
                return new TopicExpectationBuilder(this, "set");
            }

            public static class TopicExpectationBuilder {
                private final MqttFunctionExpectationBuilder mqttFunctionBuilder;
                private final String messageType;


                public TopicExpectationBuilder(MqttFunctionExpectationBuilder mqttFunctionBuilder, String messageType) {
                    this.mqttFunctionBuilder = mqttFunctionBuilder;
                    this.messageType = messageType;
                }

                public MessageExpectationBuilder toHave() {
                    return new MessageExpectationBuilder(this);
                }

                public static class MessageExpectationBuilder {
                    private final TopicExpectationBuilder topicBuilder;

                    public MessageExpectationBuilder(TopicExpectationBuilder topicBuilder) {
                        this.topicBuilder = topicBuilder;
                    }

                    public StateExpectationBuilder state() {
                        return new StateExpectationBuilder(this);
                    }

                    public static class StateExpectationBuilder {
                        private final MessageExpectationBuilder messageBuilder;

                        public StateExpectationBuilder(MessageExpectationBuilder messageBuilder) {
                            this.messageBuilder = messageBuilder;
                        }

                        public void on() {
                            value("ON");

                        }

                        public void off() {
                            value("OFF");
                        }

                        private void value(String state) {
                            this.match("state: '" + state + "'", m -> Objects.equals(m.getComponentState().getState(), state));
                        }

                        public void match(String describe, Predicate<MqttCapture> matcher) {
                            String topic = "test_prefix_teletask2mqtt/MAN_TEST_localhost_1234/" + messageBuilder.topicBuilder.mqttFunctionBuilder.function.toString().toLowerCase() + "/" + messageBuilder.topicBuilder.mqttFunctionBuilder.number + "/" + messageBuilder.topicBuilder.messageType;

                            String msg = AnsiOutput.toString("[%s]", AnsiColor.DEFAULT, " Message in topic '", AnsiColor.BRIGHT_CYAN, topic, AnsiColor.DEFAULT, "' expected to have: ", AnsiColor.BRIGHT_YELLOW, describe, AnsiColor.DEFAULT);

                            AtomicReference<MqttCapture> capture = new AtomicReference<>();
                            try {
                                Awaitility.await(describe)
                                        .pollInterval(250, TimeUnit.MILLISECONDS)
                                        .atMost(10, TimeUnit.SECONDS)
                                        .until(() -> {
                                            Optional<MqttCapture> mqttCapture = this.messageBuilder.topicBuilder.mqttFunctionBuilder.mqttBuilder.mqttContainer.captures.stream()
                                                    .sorted(Comparator.comparing(MqttCapture::getTimestamp).reversed())
                                                    .filter(m -> Objects.equals(m.getTopic(), topic))
                                                    .findFirst();
                                            capture.set(mqttCapture.orElse(null));
                                            return mqttCapture.map(matcher::test).orElse(false);
                                        });
                            } catch (Exception e) {
                                LOGGER.error(AnsiOutput.toString(AnsiColor.BRIGHT_RED, String.format(msg, "FAILED"), " - but was: ", AnsiColor.RED, capture.get(), AnsiColor.DEFAULT));
                                throw e;
                            }

                            LOGGER.info(AnsiOutput.toString(AnsiColor.BRIGHT_GREEN, String.format(msg, "SUCCESS")));
                        }
                    }

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
