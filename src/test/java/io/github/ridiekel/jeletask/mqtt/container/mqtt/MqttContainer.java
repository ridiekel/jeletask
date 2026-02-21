package io.github.ridiekel.jeletask.mqtt.container.mqtt;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.DimmerStateCalculator;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.InputStateCalculator;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.MotorStateCalculator;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.DimmerState;
import io.github.ridiekel.jeletask.mqtt.container.RedirectingSlf4jLogConsumer;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessor;
import io.github.ridiekel.jeletask.utilities.StringUtilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class MqttContainer extends GenericContainer<MqttContainer> {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Teletask2MqttConfigurationProperties properties;
    private final MqttProcessor mqttProcessor;
    private final CentralUnit centralUnit;
    private MqttClient mqttClient;

    private final List<MqttCapture> captures = new ArrayList<>();

    public MqttContainer(Teletask2MqttConfigurationProperties properties, MqttProcessor mqttProcessor, CentralUnit centralUnit) {
        super(DockerImageName.parse("eclipse-mosquitto:latest"));
        this.properties = properties;
        this.mqttProcessor = mqttProcessor;
        this.centralUnit = centralUnit;
        this.withExposedPorts(1883)
                .withStartupTimeout(Duration.of(5, ChronoUnit.MINUTES))
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
        this.properties.getMqtt().setHost(getHost());

        String broker = "tcp://" + this.getHost() + ":" + this.getPort();
        LOGGER.info(AnsiOutput.toString(AnsiColor.BRIGHT_MAGENTA, "MQTT expected to start on address: ", AnsiColor.BRIGHT_WHITE, broker, AnsiColor.DEFAULT));

        try {
            mqttClient = new MqttClient(broker, "mqtt-test-capture-client", new MemoryPersistence());
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }

        Awaitility.await("MQTT Test Client Connected").atMost(5, TimeUnit.MINUTES).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            boolean connected;
            try {
                MqttConnectOptions options = new MqttConnectOptions();
                options.setAutomaticReconnect(true);
                options.setCleanSession(true);
                options.setKeepAliveInterval(5);
                mqttClient.connect(options);
                connected = mqttClient.isConnected();
            } catch (MqttException e) {
                connected = false;
            }
            return connected;
        });

        this.followOutput(new RedirectingSlf4jLogConsumer(this.getClass(), AnsiColor.BLUE, "mqtt-container-log"));

        LOGGER.info(AnsiOutput.toString(AnsiColor.BRIGHT_GREEN, "MQTT started and test client connected", AnsiColor.DEFAULT));
    }

    public MqttProcessor processor() {
        return mqttProcessor;
    }

    public void startCapturing() {
        LOGGER.info(() -> AnsiOutput.toString(AnsiColor.BRIGHT_MAGENTA, "Starting MQTT capturing", AnsiColor.DEFAULT));
        this.subscribe("#", (t, m) -> {
            String message = new String(m.getPayload());

            String prettyMessage;
            try {
                prettyMessage = StringUtilities.prettyString(message);
            } catch (Exception e) {
                prettyMessage = message;
            }
            prettyMessage = StringUtilities.indent(prettyMessage);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(AnsiOutput.toString(
                        AnsiColor.BRIGHT_GREEN, "Captured\n",
                        AnsiColor.BRIGHT_BLUE, "\t" + t, AnsiColor.DEFAULT, "\n",
                        AnsiColor.BRIGHT_YELLOW, prettyMessage, AnsiColor.DEFAULT, "\n",
                        AnsiColor.BRIGHT_CYAN, "\tmosquitto_pub -h " + this.getHost() + " -p ", this.getPort(), " \\\n\t\t-t '", t, "' \\\n\t\t-m '", m, "'", AnsiColor.DEFAULT));
            } else if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(AnsiOutput.toString(
                        AnsiColor.BRIGHT_GREEN, "Captured",
                        AnsiColor.BRIGHT_BLUE, " - " + t, AnsiColor.DEFAULT, " - ",
                        AnsiColor.BRIGHT_YELLOW, message, AnsiColor.DEFAULT, " - ",
                        AnsiColor.BRIGHT_CYAN, "mosquitto_pub -h " + this.getHost() + " -p ", this.getPort(), " -t '", t, "' -m '", message.trim(), "'", AnsiColor.DEFAULT));
            }

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
        LOGGER.info(AnsiOutput.toString(AnsiColor.BRIGHT_MAGENTA, "Resetting captured messages", AnsiColor.DEFAULT));
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

        public MqttFunctionExpectationBuilder localmood(int number) {
            return function(Function.LOCMOOD, number);
        }

        public MqttFunctionExpectationBuilder generalmood(int number) {
            return function(Function.GENMOOD, number);
        }

        public MqttFunctionExpectationBuilder condition(int number) {
            return function(Function.COND, number);
        }

        public MqttFunctionExpectationBuilder flag(int number) {
            return function(Function.FLAG, number);
        }

        public MqttFunctionExpectationBuilder input(int number) {
            return function(Function.INPUT, number);
        }

        public MqttFunctionExpectationBuilder sensor(int number) {
            return function(Function.SENSOR, number);
        }

        public MqttFunctionExpectationBuilder motor(int number) {
            return function(Function.MOTOR, number);
        }

        public MqttFunctionExpectationBuilder dimmer(int number) {
            return function(Function.DIMMER, number);
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
                return lastStateMessage(0);
            }

            public TopicExpectationBuilder lastStateMessage(int index) {
                return new TopicExpectationBuilder(this, index, "state");
            }

            public TopicExpectationBuilder lastSetMessage() {
                return lastSetMessage(0);
            }

            public TopicExpectationBuilder lastSetMessage(int index) {
                return new TopicExpectationBuilder(this, index, "set");
            }

            public static class TopicExpectationBuilder {
                private final MqttFunctionExpectationBuilder mqttFunctionBuilder;
                private final int index;
                private final String messageType;

                public TopicExpectationBuilder(MqttFunctionExpectationBuilder mqttFunctionBuilder, int index, String messageType) {
                    this.mqttFunctionBuilder = mqttFunctionBuilder;
                    this.index = index;
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

                        public void brightness(Integer expected) {
                            this.match("expected (Integer): '" + expected + "'", m -> {
                                Integer actual = ((DimmerState) m.getComponentState(
                                        this.messageBuilder.topicBuilder.mqttFunctionBuilder.mqttBuilder.mqttContainer.centralUnit,
                                        this.messageBuilder.topicBuilder.mqttFunctionBuilder.function,
                                        this.messageBuilder.topicBuilder.mqttFunctionBuilder.number
                                )).getBrightness();
                                return new MatchTest.MatchTestResult(Objects.equals(actual, expected), Optional.ofNullable(actual).map(Objects::toString).orElse(null));
                            });
                        }

                        public void dimmerOn() {
                            state(DimmerStateCalculator.ValidDimmerState.ON);
                        }

                        public void dimmerOff() {
                            state(DimmerStateCalculator.ValidDimmerState.OFF);
                        }

                        public void on() {
                            state(OnOffToggleStateCalculator.ValidOnOffToggle.ON);
                        }

                        public void off() {
                            state(OnOffToggleStateCalculator.ValidOnOffToggle.OFF);
                        }

                        public void value(String expected) {
                            this.match("expected (string): " + expected, m -> {
                                State<?> componentState = m.getComponentState(
                                        this.messageBuilder.topicBuilder.mqttFunctionBuilder.mqttBuilder.mqttContainer.centralUnit,
                                        this.messageBuilder.topicBuilder.mqttFunctionBuilder.function,
                                        this.messageBuilder.topicBuilder.mqttFunctionBuilder.number
                                );
                                String state = componentState.getState().toString();
                                return new MatchTest.MatchTestResult(Objects.equals(state, expected), state);
                            });
                        }

                        public void inputClosed() {
                            state(InputStateCalculator.ValidInputState.CLOSED);
                        }

                        public void notPressed() {
                            state(InputStateCalculator.ValidInputState.NOT_PRESSED);
                        }

                        public void inputOpen() {
                            state(InputStateCalculator.ValidInputState.OPEN);
                        }

                        public void motorDown() {
                            state(MotorStateCalculator.ValidMotorDirectionState.DOWN);
                        }

                        public void motorUp() {
                            state(MotorStateCalculator.ValidMotorDirectionState.UP);
                        }

                        public void motorGotoPosition() {
                            state(MotorStateCalculator.ValidMotorDirectionState.MOTOR_GO_TO_POSITION);
                        }

                        public void shortPress() {
                            state(InputStateCalculator.ValidInputState.SHORT_PRESS);
                        }

                        public void longPress() {
                            state(InputStateCalculator.ValidInputState.LONG_PRESS);
                        }

                        private void state(Enum<?> expected) {
                            this.match("expected (enum): " + expected.describeConstable().orElse(null), m -> {
                                Enum<?> actual = (Enum<?>) m.getComponentState(
                                        this.messageBuilder.topicBuilder.mqttFunctionBuilder.mqttBuilder.mqttContainer.centralUnit,
                                        this.messageBuilder.topicBuilder.mqttFunctionBuilder.function,
                                        this.messageBuilder.topicBuilder.mqttFunctionBuilder.number
                                ).getState();
                                return new MatchTest.MatchTestResult(Objects.equals(actual, expected), actual.describeConstable().map(Objects::toString).orElse(null));
                            });
                        }

                        public void match(String describe, MatchTest matcher) {
                            String topic = "test_prefix_teletask2mqtt/MAN_TEST_localhost/" + messageBuilder.topicBuilder.mqttFunctionBuilder.function.toString().toLowerCase() + "/" + messageBuilder.topicBuilder.mqttFunctionBuilder.number + "/" + messageBuilder.topicBuilder.messageType;

                            String msg = AnsiOutput.toString("[%s]", AnsiColor.DEFAULT, " Message in topic '", AnsiColor.BRIGHT_CYAN, topic, AnsiColor.DEFAULT, "' expected to have: ", AnsiColor.BRIGHT_YELLOW, describe, AnsiColor.DEFAULT);

                            AtomicReference<MqttCapture> capture = new AtomicReference<>();
                            AtomicReference<String> actual = new AtomicReference<>();
                            try {
                                Awaitility.await(describe)
                                        .pollInterval(250, TimeUnit.MILLISECONDS)
                                        .atMost(10, TimeUnit.SECONDS)
                                        .until(() -> {
                                            List<MqttCapture> captureList = this.messageBuilder.topicBuilder.mqttFunctionBuilder.mqttBuilder.mqttContainer.captures.stream()
                                                    .sorted(Comparator.comparing(MqttCapture::getTimestamp).reversed())
                                                    .filter(m -> Objects.equals(m.getTopic(), topic))
                                                    .toList();
                                            if (captureList.size() > this.messageBuilder.topicBuilder.index) {
                                                MqttCapture mqttCapture = captureList.get(this.messageBuilder.topicBuilder.index);
                                                capture.set(mqttCapture);
                                                MatchTest.MatchTestResult test = matcher.test(mqttCapture);
                                                actual.set(test.actual());
                                                return test.result();
                                            } else {
                                                return false;
                                            }
                                        });// 0 1 2 3    1 2 3 4
                            } catch (Exception e) {
                                LOGGER.error(AnsiOutput.toString(AnsiColor.BRIGHT_RED, String.format(msg, "FAILED"), " - but was: ", AnsiColor.RED, actual.get(), "\n", capture.get(), AnsiColor.DEFAULT));
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

        @Override
        public String toString() {
            try {
                return StringUtilities.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public State getComponentState(CentralUnit centralUnit, Function function, int number) {
            return centralUnit.getMessageHandler().getFunctionConfig(function).getStateCalculator(centralUnit.getComponent(function, number)).stateFromMessage(message);
        }
    }
}
