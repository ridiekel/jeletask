package io.github.ridiekel.jeletask.mqtt.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.client.listener.StateChangeListener;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.mqtt.listener.command.MQTTMessageToTeletaskCommand;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HomeAssistentAutoConfig;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.logic.input.LongPressInputCaptor;
import io.github.ridiekel.jeletask.mqtt.listener.logic.motor.MotorProgressor;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.service.MqttMessageTraceService;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.sse.centralunit.CentralUnitSsePublisher;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.sse.centralunit.CentralUnitUpdate;
import io.github.ridiekel.jeletask.utilities.StringUtilities;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.context.MessageSource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static io.github.ridiekel.jeletask.mqtt.listener.MqttLogger.*;
import static io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfigParameters.componentTopic;

@Service
public class MqttProcessor implements StateChangeListener {
    private static final Logger LOG = LogManager.getLogger();

    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_-]");
    public static final int MQTT_CONNECTION_TIMEOUT_SECONDS = 5;

    private final CentralUnit centralUnit;
    private final TeletaskClient teletaskClient;
    private final MqttMessageTraceService traceService;
    private static final ConnectedStatus CONNECTED_STATUS = new ConnectedStatus();
    private final CentralUnitSsePublisher centralUnitSsePublisher;
    private final HomeAssistentAutoConfig homeAssistentAutoConfig;
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private final MessageSource messageSource;

    private PingMesage pingMesage;
    private MqttClient mqttClient;
    @Getter
    private final String prefix;
    private final MqttConnectOptions connOpts;
    @Getter
    private final String teletaskIdentifier;

    private final MotorProgressor motorProgressor;
    private final LongPressInputCaptor longPressInputCaptor;
    @Getter
    private final Teletask2MqttConfigurationProperties configuration;

    public MqttProcessor(CentralUnit centralUnit,
                         TeletaskClient teletaskClient,
                         Teletask2MqttConfigurationProperties configuration,
                         MqttMessageTraceService traceService,
                         CentralUnitSsePublisher centralUnitSsePublisher, MessageSource messageSource) {
        this.centralUnit = centralUnit;
        this.teletaskClient = teletaskClient;
        this.traceService = traceService;
        this.centralUnitSsePublisher = centralUnitSsePublisher;
        this.teletaskClient.registerStateChangeListener(this);
        this.configuration = configuration;
        this.motorProgressor = new MotorProgressor(configuration.getPublish().getMotorPositionInterval(), this::publishState);
        this.longPressInputCaptor = new LongPressInputCaptor(this);

        String username = Optional.ofNullable(configuration.getMqtt().getUsername()).map(String::trim).filter(u -> !u.isEmpty()).orElse(null);
        char[] password = Optional.ofNullable(configuration.getMqtt().getPassword()).map(String::toCharArray).orElse(null);
        this.prefix = removeInvalid(configuration.getMqtt().getPrefix(), this.configuration.getMqtt().getClientId());

        this.teletaskIdentifier = resolveTeletaskIdentifier(centralUnit, configuration);

        LOG.info(() -> String.format("teletask id: '%s'", this.teletaskIdentifier));
        LOG.info(() -> String.format("host: '%s'", this.configuration.getMqtt().getHost()));
        LOG.info(() -> String.format("port: '%s'", this.configuration.getMqtt().getPort()));
        LOG.info(() -> String.format("username: '%s'", Optional.ofNullable(username).orElse("<not specified>")));
        LOG.info(() -> String.format("clientId: '%s'", this.configuration.getMqtt().getClientId()));
        LOG.info(() -> String.format("retained: '%s'", configuration.getMqtt().isRetained()));
        LOG.info(() -> String.format("prefix: '%s'", this.prefix));
        LOG.info(() -> String.format("Remote stop topic: '%s'", this.getRemoteStopTopic()));
        LOG.info(() -> String.format("Remote refresh states topic: '%s'", this.getRemoteRefreshTopic()));

        this.connOpts = new MqttConnectOptions();
        this.connOpts.setMaxInflight(100000);

        // Do we need CleanSession?
        // I have disabled it for now because it's not subscribing to the mqtt topics again
        // after a reconnect (for ex. when you restart your mqtt broker).
        this.connOpts.setCleanSession(false);

        this.connOpts.setConnectionTimeout(MQTT_CONNECTION_TIMEOUT_SECONDS);
        this.connOpts.setAutomaticReconnect(true);
        this.connOpts.setUserName(username);
        this.connOpts.setPassword(password);

        this.homeAssistentAutoConfig = new HomeAssistentAutoConfig(this.configuration, this.centralUnit, this.getBaseTopic(), this.teletaskIdentifier);

        new Timer("motor-service").schedule(motorProgressor, 0, configuration.getPublish().getMotorPositionInterval());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.teletaskClient.disconnect();
            this.checkAndPublishConnectedStatus();
        }));
        this.messageSource = messageSource;
    }

    private void scheduleGroupGet(Teletask2MqttConfigurationProperties configuration) {
        if (configuration.getPublish().getStatesInterval() > 0) {
            LOG.info(() -> String.format("Scheduling force refresh every %s second(s)", configuration.getPublish().getStatesInterval()));
            long intervalMillis = TimeUnit.SECONDS.toMillis(configuration.getPublish().getStatesInterval());
            new Timer("groupget-service").schedule(new TimerTask() {
                @Override
                public void run() {
                    LOG.info(() -> "Forcing refresh of states...");
                    teletaskClient.groupGet();
                }
            }, intervalMillis, intervalMillis);
        }
    }

    @EventListener(classes = {ContextRefreshedEvent.class})
    @Order(200)
    public void reconnect() {
        this.configureAndConnectMqtt();
        this.teletaskClient.groupGet();
        scheduleGroupGet(configuration);
    }

    private static String resolveTeletaskIdentifier(CentralUnit centralUnit, Teletask2MqttConfigurationProperties configuration) {
        return removeInvalid(configuration.getCentral().getId(), removeInvalid(centralUnit.getHost() + "_" + centralUnit.getPort(), "impossible"));
    }

    private static String removeInvalid(String value, String defaultValue) {
        return Optional.ofNullable(value).map(String::trim).filter(u -> !u.isEmpty()).map(u -> INVALID_CHARS.matcher(u).replaceAll("_")).orElseGet(() -> removeInvalid(defaultValue, "<not_found>"));
    }

    private void configureAndConnectMqtt() {
        String broker = "tcp://" + this.configuration.getMqtt().getHost() + ":" + this.configuration.getMqtt().getPort();
        LOG.info(() -> "Connecting to MQTT broker...");

        try {
            tryDisconnect();
            this.mqttClient = new MqttClient(broker, this.configuration.getMqtt().getClientId(), new MemoryPersistence());
        } catch (MqttException e) {
            throw new IllegalStateException(e);
        }
        Awaitility.await().atMost(MQTT_CONNECTION_TIMEOUT_SECONDS, TimeUnit.MINUTES).until(() -> {
            this.connectAndWaitForMqtt();
            this.publishConfig();
            this.subscribe();
            return true;
        });
    }

    private void tryDisconnect() {
        if (this.mqttClient != null) {
            try {
                this.mqttClient.disconnect(5000);
            } catch (Exception e) {
                try {
                    this.mqttClient.disconnectForcibly(5000, 5000);
                } catch (MqttException ex) {
                    LOG.error(() -> "Could not disconnect cleanly from MQTT broker.", ex);
                }
            }
        }
    }

    private void subscribe() throws MqttException {
        LOG.info(() -> "Subscribing to topics...");
        this.mqttClient.subscribe(this.getBaseTopic() + "/+/+/set", 0,
                toTracingListener((topic, message) -> {
                    EXECUTOR.execute(() -> {
                        new MQTTMessageToTeletaskCommand(this.teletaskClient, this.prefix, this.teletaskIdentifier).messageArrived(topic, message);
                    });
                })
        );
        this.mqttClient.subscribe(getPingTopic(), 0,
                toTracingListener((topic, message) -> {
                    this.pingMesage.setReceived(Instant.now());
                })
        );
        this.mqttClient.subscribe(getRemoteStopTopic(), 0, toTracingListener((topic, message) -> {
            LOG.debug(() -> String.format("Stopping the service: %s", Optional.ofNullable(message).map(MqttMessage::toString).orElse("<no reason provided>")));
            System.exit(0);
        }));
        this.mqttClient.subscribe(getRemoteRefreshTopic(), 0, toTracingListener((topic, message) -> {
            LOG.debug(() -> String.format("Refreshing states: %s", Optional.ofNullable(message).map(MqttMessage::toString).orElse("<no reason provided>")));
            EXECUTOR.execute(this.teletaskClient::groupGet);
        }));
        this.mqttClient.subscribe("homeassistant/status", 0, toTracingListener((topic, message) -> {
            LOG.debug(() -> String.format("Home assistent 'birth'/'last will' message: %s", message));
            if (message != null && Objects.equals(message.toString(), "online")) {
                long seconds = ThreadLocalRandom.current().nextInt(2, 6);
                LOG.info(AnsiOutput.toString(AnsiColor.BRIGHT_WHITE, "Received online message from Home Assistant, sending config and refreshing states in " + seconds + " seconds", AnsiColor.DEFAULT));
                EXECUTOR.schedule(() -> {
                    this.publishConfig();
                    this.publishConnectionStatus();
                    this.teletaskClient.groupGet();
                }, seconds, TimeUnit.SECONDS);
            }
        }));
    }

    private String getPingTopic() {
        return this.getBaseTopic() + "/ping";
    }

    private IMqttMessageListener toTracingListener(IMqttMessageListener target) {
        return (topic, message) -> {
            this.traceService.receive(topic, new String(message.getPayload(), StandardCharsets.UTF_8), message.getQos(), message.isRetained());
            target.messageArrived(topic, message);
        };
    }

    private String getRemoteStopTopic() {
        return getBaseTopic() + "/stop";
    }

    private String getRemoteRefreshTopic() {
        return getBaseTopic() + "/refresh";
    }

    private synchronized void connectAndWaitForMqtt() {
        while (!this.mqttClient.isConnected()) {
            try {
                this.mqttClient.connect(this.connOpts);
            } catch (MqttException e) {
                LOG.debug(() -> String.format("Connect warning: %s", e.getMessage()));
            }

            try {
                Awaitility.await().pollDelay(1, TimeUnit.SECONDS).atMost(MQTT_CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS).until(() -> {
                    LOG.info(() -> "Waiting for mqtt connection...");
                    return this.mqttClient.isConnected();
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void publishConfig() {
        LOG.info(() -> "Publishing config...");
        this.centralUnit.getAllComponents().forEach(component -> {
            this.homeAssistentAutoConfig.toConfig(component)
                    .forEach((topic, message) -> {
                        this.publish(
                                What.CONFIG,
                                getLoggingStringForComponent(component),
                                topic,
                                message,
                                this.getConfiguration().getLog().isHaconfigEnabled() ? LOG::info : LOG::debug
                        );
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
        });
        checkAndPublishConnectedStatus();
    }

    @Scheduled(fixedDelay = MQTT_CONNECTION_TIMEOUT_SECONDS, timeUnit = TimeUnit.SECONDS)
    public void checkAndPublishConnectedStatus() {
        boolean currentConnectionStatus = this.teletaskClient.isConnected();
        if (currentConnectionStatus != CONNECTED_STATUS.connected) {
            CONNECTED_STATUS.connected = currentConnectionStatus;
            CONNECTED_STATUS.since = Instant.now();
            CONNECTED_STATUS.nextPublish = Instant.now().plus(1, ChronoUnit.MINUTES);
            publishConnectionStatus();
        } else if (CONNECTED_STATUS.nextPublish.isBefore(Instant.now())) {
            CONNECTED_STATUS.nextPublish = Instant.now().plus(1, ChronoUnit.MINUTES);
            publishConnectionStatus();
        }
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES, initialDelayString = "15S")
    public void pingMqtt() {
        try {
            this.pingMesage = new PingMesage(Instant.now());
            this.publish(What.PING, () -> padded("SEND"), getPingTopic(), StringUtilities.OBJECT_MAPPER.writeValueAsString(pingMesage), LOG::debug);
            Awaitility.await("Waiting for ping response").atMost(MQTT_CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS).pollInterval(10, TimeUnit.MILLISECONDS).until(() -> Objects.nonNull(this.pingMesage.getReceived()));
            LOG.debug(String.format(WHAT_LOG_PATTERNS.get(What.PING), getWhat(What.PING), AnsiOutput.toString(padded("CHECK"), " - ", AnsiColor.BLUE, this.pingMesage.received.toEpochMilli() - this.pingMesage.sent.toEpochMilli() + "ms"), StringUtilities.OBJECT_MAPPER.writeValueAsString(pingMesage)));
        } catch (Exception e) {
            LOG.error(() -> "MQTT ping failed: " + e.getMessage());
            reconnect();
        }
    }

    private static String padded(String msg) {
        return "[" + StringUtils.rightPad(msg, 10) + "]";
    }

    @Getter
    @Setter
    private static final class PingMesage {
        private Instant sent;
        private Instant received;

        public PingMesage(Instant sent) {
            this.sent = sent;
        }
    }

    private void publishConnectionStatus() {
        try {
            LOG.debug("Publishing connection status: " + State.OBJECT_MAPPER.writeValueAsString(CONNECTED_STATUS));
            this.publish(What.ONLINE, () -> padded("BRIDGE"), HAConfigParameters.availabilityTopic(this.getBaseTopic()), CONNECTED_STATUS.getState(), LOG::info);
        } catch (JsonProcessingException e) {
            LOG.error(e);
        }
    }

    @Override
    public void receive(List<ComponentSpec> components) {
        components.forEach(c -> {
            if (c.getFunction() == Function.MOTOR) {
                this.motorProgressor.update(c);
            } else if (c.getFunction() == Function.INPUT && c.getLong_press_duration_millis() != null) {
                this.longPressInputCaptor.update(c);
            } else {
                publishState(c, c.getState());
            }

            this.centralUnitSsePublisher.publish(new CentralUnitUpdate(c.getFunction(), c.getNumber(), c.getState()));
        });
    }

    public void publishState(ComponentSpec componentSpec, State<?> state) {
        String ttTopic = componentTopic(this.getBaseTopic(), componentSpec) + "/state";
        this.publish(MqttLogger.What.PUBLISH, getLoggingStringForComponent(componentSpec), ttTopic, state.toString(), LOG::info);
    }

    private void publish(MqttLogger.What what, Supplier<String> logString, String topic, String message, Consumer<String> level) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(0);
            mqttMessage.setRetained(this.getConfiguration().getMqtt().isRetained());

            this.connectAndWaitForMqtt();

            String logStringValue = logString.get();

            level.accept(String.format(WHAT_LOG_PATTERNS.get(what), getWhat(what), logStringValue, topicToLogWithColors(topic) + payloadToLogWithColors(message)));
            this.mqttClient.publish(topic, mqttMessage);
            this.traceService.publish(topic, message, mqttMessage.getQos(), mqttMessage.isRetained());
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    private String getBaseTopic() {
        return this.prefix + "/" + this.teletaskIdentifier;
    }

    @Override
    public void stop() {
        try {
            this.mqttClient.disconnect();
        } catch (MqttException e) {
            LOG.debug(e::getMessage);
        }
    }

    private static class ConnectedStatus {
        @Getter
        private boolean connected;
        @Getter
        private Instant since = Instant.now();
        private Instant nextPublish = Instant.now();

        @SuppressWarnings("unused")
        public String getState() {
            return isConnected() ? "online" : "offline";
        }
    }

    private String topicToLogWithColors(String topic) {
        return this.getConfiguration().getLog().isTopicEnabled() ? AnsiOutput.toString(AnsiColor.MAGENTA, "[" + StringUtils.rightPad(topic, 60) + "] - ", AnsiColor.DEFAULT) : "";
    }

}
