package io.github.ridiekel.jeletask.mqtt.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.SensorType;
import io.github.ridiekel.jeletask.client.listener.StateChangeListener;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.DisplayMessageState;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfig;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.types.*;
import io.github.ridiekel.jeletask.mqtt.listener.logic.input.LongPressInputCaptor;
import io.github.ridiekel.jeletask.mqtt.listener.logic.motor.MotorProgressor;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.service.MqttMessageTraceService;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.sse.centralunit.CentralUnitSsePublisher;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.sse.centralunit.CentralUnitUpdate;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfigParameters.componentTopic;

@Service
public class MqttProcessor implements StateChangeListener {
    private static final Logger LOG = LogManager.getLogger();

    public static final int TIMEOUT = 30;

    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_-]");
    private static final Map<What, String> WHAT_LOG_PATTERNS = Map.of(
            What.PUBLISH, whatPattern(AnsiColor.YELLOW),
            What.RECEIVE, whatPattern(AnsiColor.MAGENTA),
            What.ONLINE, whatPattern(AnsiColor.BRIGHT_WHITE),
            What.CONFIG, whatPattern(AnsiColor.CYAN),
            What.DELETE, whatPattern(AnsiColor.BLUE)
    );

    private static String whatPattern(AnsiColor color) {
        return AnsiOutput.toString(color, "[MQTT      ] - [%s] - %s", AnsiColor.DEFAULT, " - %s");
    }

    private final CentralUnit centralUnit;
    private final TeletaskClient teletaskClient;
    private final MqttMessageTraceService traceService;
    private static final ConnectedStatus CONNECTED_STATUS = new ConnectedStatus();
    private final CentralUnitSsePublisher centralUnitSsePublisher;


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
    public void onApplicationEvent() {
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
            this.mqttClient = new MqttClient(broker, this.configuration.getMqtt().getClientId(), new MemoryPersistence());
        } catch (MqttException e) {
            throw new IllegalStateException(e);
        }
        Awaitility.await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> {
            this.connectMqtt();
            this.publishConfig();
            this.subscribe();
            return true;
        });
    }

    private void subscribe() throws MqttException {
        LOG.info(() -> "Subscribing to topics...");
        this.mqttClient.subscribe(this.prefix + "/" + this.teletaskIdentifier + "/+/+/set", 0,
                tracingListener(new ChangeStateMqttCallback())
        );
        this.mqttClient.subscribe(remoteStopTopic(), 0, tracingListener((topic, message) -> {
            LOG.debug(() -> String.format("Stopping the service: %s", Optional.ofNullable(message).map(MqttMessage::toString).orElse("<no reason provided>")));
            System.exit(0);
        }));
        this.mqttClient.subscribe(remoteRefreshTopic(), 0, tracingListener((topic, message) -> {
            LOG.debug(() -> String.format("Refreshing states: %s", Optional.ofNullable(message).map(MqttMessage::toString).orElse("<no reason provided>")));
            this.teletaskClient.groupGet();
        }));
    }

    private IMqttMessageListener tracingListener(IMqttMessageListener target) {
        return (topic, message) -> {
            this.traceService.receive(topic, new String(message.getPayload(), StandardCharsets.UTF_8), message.getQos(), message.isRetained());
            target.messageArrived(topic, message);
        };
    }

    public MqttProcessor(CentralUnit centralUnit,
                         TeletaskClient teletaskClient,
                         Teletask2MqttConfigurationProperties configuration,
                         MqttMessageTraceService traceService, CentralUnitSsePublisher centralUnitSsePublisher
    ) {
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
        LOG.info(() -> String.format("Remote stop topic: '%s'", this.remoteStopTopic()));
        LOG.info(() -> String.format("Remote refresh states topic: '%s'", this.remoteRefreshTopic()));

        this.connOpts = new MqttConnectOptions();
        this.connOpts.setMaxInflight(100000);

        // Do we need CleanSession?
        // I have disabled it for now because it's not subscribing to the mqtt topics again
        // after a reconnect (for ex. when you restart your mqtt broker).
        this.connOpts.setCleanSession(false);

        this.connOpts.setAutomaticReconnect(true);
        this.connOpts.setUserName(username);
        this.connOpts.setPassword(password);

        new Timer("motor-service").schedule(motorProgressor, 0, configuration.getPublish().getMotorPositionInterval());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.teletaskClient.disconnect();
            this.checkAndPublishConnectedStatus();
        }));
    }

    private String remoteStopTopic() {
        return this.prefix + "/" + this.teletaskIdentifier + "/stop";
    }

    private String remoteRefreshTopic() {
        return this.prefix + "/" + this.teletaskIdentifier + "/refresh";
    }

    private synchronized void connectMqtt() {
        if (!this.mqttClient.isConnected()) {
            try {
                this.mqttClient.connect(this.connOpts);
            } catch (MqttException e) {
                LOG.debug(() -> String.format("Connect warning: %s", e.getMessage()));
            }

            Awaitility.await().pollDelay(100, TimeUnit.MILLISECONDS).atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> {
                LOG.info(() -> "Waiting for mqtt connection...");
                return this.mqttClient.isConnected();
            });
        }
    }

    public void refresh() {
        this.checkAndPublishConnectedStatus();
    }

    private void publishConfig(ComponentSpec c) {
        this.toConfig(c).forEach((t, m) -> this.publish(What.CONFIG, getLoggingStringForComponent(c), t, m, this.getConfiguration().getLog().isHaconfigEnabled() ? LOG::info : LOG::debug));
    }

    public void publishConfig() {
        LOG.info(() -> "Publishing config...");
        this.centralUnit.getAllComponents().forEach(this::publishConfig);
        checkAndPublishConnectedStatus();
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
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

    private void publishConnectionStatus() {
        try {
            LOG.debug("Publishing connection status: " + State.OBJECT_MAPPER.writeValueAsString(CONNECTED_STATUS));
            this.publish(What.ONLINE, () -> "Bridge", HAConfigParameters.availabilityTopic(this.baseTopic()), CONNECTED_STATUS.getState(), LOG::info);
        } catch (JsonProcessingException e) {
            LOG.error(e);
        }
    }

    private String payloadToLogWithColors(String payload) {
        return AnsiOutput.toString(AnsiColor.GREEN, payload, AnsiColor.DEFAULT);
    }

    private Map<String, String> toConfig(ComponentSpec component) {
        return Optional.ofNullable(component)
                .flatMap(c -> Optional.ofNullable(FUNCTION_TO_TYPE.get(c.getFunction()))
                        .map(f -> f.getConfigTopicsAndMessages(this.centralUnit, c, this.baseTopic(), this.teletaskIdentifier, this.haDiscoveryPrefix()))
                ).orElse(new HashMap<>());
    }

    private String haDiscoveryPrefix() {
        return Optional.ofNullable(this.getConfiguration().getMqtt().getDiscoveryPrefix()).orElse("homeassistant");
    }

    private static final Map<Function, FunctionConfig> FUNCTION_TO_TYPE = Map.ofEntries(
            // COND and INPUT are readonly -> HA autodiscovery: binary_sensor
            Map.entry(Function.COND, f(HADeviceType.BINARY_SENSOR, HABinarySensorConfig::new)),
            Map.entry(Function.INPUT, f(HADeviceType.SENSOR, HAInputTriggerConfig::new)),
            // Dimmers -> -> HA auto discovery: light
            Map.entry(Function.DIMMER, f(HADeviceType.LIGHT, HADimmerConfig::new)),
            // Flags can be read + turned on/off -> HA auto discovery: switch
            Map.entry(Function.FLAG, f(HADeviceType.SWITCH, HASwitchConfig::new)),
            // Mood functions actually act like a switch in Teletask. They can be turned ON/OFF?
            // HA scenes can only be 'activated' and do not support a state?
            // -> HA auto discovery: switch
            Map.entry(Function.GENMOOD, f(c -> {
                String type = Optional.ofNullable(c.getType()).orElse("switch").toLowerCase();
                return Objects.equals(type, "switch") ? HADeviceType.SWITCH : HADeviceType.SCENE;
            }, HASwitchConfig::new)),
            Map.entry(Function.LOCMOOD, f(c -> {
                String type = Optional.ofNullable(c.getType()).orElse("switch").toLowerCase();
                return Objects.equals(type, "switch") ? HADeviceType.SWITCH : HADeviceType.SCENE;
            }, HASwitchConfig::new)),
            Map.entry(Function.TIMEDMOOD, f(HADeviceType.SWITCH, HASwitchConfig::new)),
            // Motors -> HA auto discovery: cover. Sufficient?
            Map.entry(Function.MOTOR, f(HADeviceType.COVER, HAMotorConfig::new)),
            Map.entry(Function.SENSOR, f(haDeviceTypeFromTypeToHaTypeMap(Map.of(
                    SensorType.TEMPERATURE, HADeviceType.SENSOR,
                    SensorType.LIGHT, HADeviceType.SENSOR,
                    SensorType.HUMIDITY, HADeviceType.SENSOR,
                    SensorType.GAS, HADeviceType.SENSOR,
                    SensorType.TEMPERATURECONTROL, HADeviceType.CLIMATE,
                    SensorType.PULSECOUNTER, HADeviceType.SENSOR
            )), HASensorConfig::new)),
            Map.entry(Function.RELAY,
                    f(c -> {
                                String type = Optional.ofNullable(c.getType()).orElse("light").toLowerCase();
                                return Objects.equals(type, "switch") ? HADeviceType.SWITCH : HADeviceType.LIGHT;
                            },
                            c -> {
                                String type = Optional.ofNullable(c.getComponentSpec().getType()).orElse("light").toLowerCase();
                                if (Objects.equals(type, "light")) {
                                    return new HALightConfig(c);
                                } else {
                                    return new HASwitchConfig(c);
                                }
                            })),
            // Timed functions actually act like a switch. They can only be ON or OFF -> HA auto discovery: switch
            Map.entry(Function.TIMEDFNC, f(HADeviceType.SWITCH, HASwitchConfig::new))
    );

    private static <T extends Enum<T>> java.util.function.Function<ComponentSpec, HADeviceType> haDeviceTypeFromTypeToHaTypeMap(Map<T, HADeviceType> map) {
        Map<String, HADeviceType> result = new HashMap<>();
        for (Map.Entry<T, HADeviceType> e : map.entrySet()) {
            result.put(String.valueOf(e.getKey()).toUpperCase(), e.getValue()); // null-veilig
        }
        return c -> result.get(c.getType().toUpperCase());
    }

    static FunctionConfig f(HADeviceType type, java.util.function.Function<HAConfigParameters, HAConfig<?>> config) {
        return f(c -> type, config);
    }

    static FunctionConfig f(java.util.function.Function<ComponentSpec, HADeviceType> type, java.util.function.Function<HAConfigParameters, HAConfig<?>> config) {
        return new FunctionConfig(type, config);
    }

    private enum HADeviceType {
        BINARY_SENSOR,
        SENSOR,
        LIGHT,
        CLIMATE,
        SWITCH,
        SCENE,
        COVER;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    private static class FunctionConfig {

        private final java.util.function.Function<ComponentSpec, String> type;
        private List<java.util.function.Function<HAConfigParameters, HAConfig<?>>> config;

        private FunctionConfig(java.util.function.Function<ComponentSpec, HADeviceType> typeIfAbsent, java.util.function.Function<HAConfigParameters, HAConfig<?>> config) {
            this.type = c -> Optional.ofNullable(c.getHAType()).orElseGet(() -> typeIfAbsent.apply(c).toString());
            this.config = List.of(config);
        }

        protected String getHAType(ComponentSpec componentSpec) {
            return this.type.apply(componentSpec);
        }

        public Map<String, String> getConfigTopicsAndMessages(CentralUnit centralUnit, ComponentSpec componentSpec, String baseTopic, String haNodeId, String haDiscoveryPrefix) {
            HAConfigParameters params = new HAConfigParameters(
                    centralUnit,
                    componentSpec,
                    baseTopic,
                    haNodeId
            );

            Map<String, String> topics = new HashMap<>();
            for (java.util.function.Function<HAConfigParameters, HAConfig<?>> c : this.config) {
                HAConfig<?> haConfig = c.apply(params);
                componentSpec.getHaPublishedConfig().add(haConfig);
                String topic = createConfigTopic(componentSpec, haNodeId, haDiscoveryPrefix);
                String message = Optional.ofNullable(haConfig).map(HAConfig::toString).orElse(null);
                topics.put(topic, message);
            }

            return topics;
        }

        private String createConfigTopic(ComponentSpec c, String haNodeId, String haDiscoveryPrefix) {
            //<discovery_prefix>/<component>/[<node_id>/]<object_id>/config
            return String.format("%s/%s/%s/%s/config",
                    haDiscoveryPrefix,
                    haComponent(c),
                    haNodeId,
                    haObjectId(c)
            );
        }

        protected String haObjectId(ComponentSpec c) {
            return c.getFunction().toString().toLowerCase() + "_" + c.getNumber();
        }

        protected String haComponent(ComponentSpec c) {
            return Optional.ofNullable(c.getHAType()).orElse(FUNCTION_TO_TYPE.get(c.getFunction()).getHAType(c));
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
        String ttTopic = componentTopic(this.baseTopic(), componentSpec) + "/state";
        this.publish(What.PUBLISH, getLoggingStringForComponent(componentSpec), ttTopic, state.toString(), LOG::info);
    }

    private void publish(What what, Supplier<String> logString, String topic, String message, Consumer<String> level) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(0);
            mqttMessage.setRetained(this.getConfiguration().getMqtt().isRetained());

            this.connectMqtt();

            String logStringValue = logString.get();

            level.accept(String.format(WHAT_LOG_PATTERNS.get(what), getWhat(what), logStringValue, topicToLogWithColors(topic) + payloadToLogWithColors(message)));
            this.mqttClient.publish(topic, mqttMessage);
            this.traceService.publish(topic, message, mqttMessage.getQos(), mqttMessage.isRetained());
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    private Supplier<String> getLoggingStringForComponent(ComponentSpec componentSpec) {
        return () -> String.format("[%s] - [%s] - [%s]", StringUtils.rightPad(componentSpec.getFunction().toString(), 10), StringUtils.leftPad(String.valueOf(componentSpec.getNumber()), 3), StringUtils.leftPad(componentSpec.getDescription(), 40));
    }

    private String getWhat(What what) {
        return StringUtils.rightPad(what.toString(), 10);
    }

    private String baseTopic() {
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

    private enum What {
        PUBLISH,
        RECEIVE,
        ONLINE,
        CONFIG,
        DELETE
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

    private class ChangeStateMqttCallback implements IMqttMessageListener {

        private final Pattern teletaskComponentPattern;

        ChangeStateMqttCallback() {
            this.teletaskComponentPattern = Pattern.compile(MqttProcessor.this.prefix + "/" + MqttProcessor.this.teletaskIdentifier + "/(\\w*)/(\\d*)/set");
        }

        @Override
        public void messageArrived(String topic, MqttMessage mqttMessage) {
            LOG.trace(() -> String.format("MQTT message arrived '%s': '%s'", topic, new String(mqttMessage.getPayload())));
            String message = mqttMessage.toString();
            try {
                Matcher matcher = this.teletaskComponentPattern.matcher(topic);
                if (matcher.find()) {
                    Function function = Function.valueOf(matcher.group(1).toUpperCase());
                    int number = Integer.parseInt(matcher.group(2));

                    State<?> state = MqttProcessor.this.centralUnit.stateFromMessage(function, number, message);

                    String componentLog = getLoggingStringForComponent(MqttProcessor.this.centralUnit.getComponent(function, number)).get();
                    LOG.info(() -> String.format(WHAT_LOG_PATTERNS.get(What.RECEIVE), getWhat(What.RECEIVE), componentLog, payloadToLogWithColors(new String(mqttMessage.getPayload()))));

                    if (function == Function.DISPLAYMESSAGE) {
                        MqttProcessor.this.teletaskClient.displaymessage(number, (DisplayMessageState) state,
                                (f, n, s) -> LOG.trace(() -> String.format("%s - MQTT topic '%s' changed state for: %s / %s -> %s", componentLog, topic, f, n, s)),
                                (f, n, s, e) -> LOG.warn(String.format("%s - MQTT topic '%s' could not change state for: %s / %s -> %s", componentLog, topic, f, n, s)));
                    } else {
                        MqttProcessor.this.teletaskClient.set(function, number, state,
                                (f, n, s) -> LOG.trace(() -> String.format("%s - MQTT topic '%s' changed state for: %s / %s -> %s", componentLog, topic, f, n, s)),
                                (f, n, s, e) -> LOG.warn(String.format("%s - MQTT topic '%s' could not change state for: %s / %s -> %s", componentLog, topic, f, n, s)));
                    }

                }
            } catch (Exception e) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(() -> String.format("MQTT topic '%s' could not change state to: %s", topic, message), e);
                } else {
                    LOG.warn(String.format("MQTT topic '%s' could not change state to: %s -- %s", topic, message, e.getMessage()), e);
                }
            }
        }
    }
}
