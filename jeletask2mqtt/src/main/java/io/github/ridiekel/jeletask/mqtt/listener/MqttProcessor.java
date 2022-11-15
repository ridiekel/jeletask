package io.github.ridiekel.jeletask.mqtt.listener;

import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.client.listener.StateChangeListener;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.mqtt.Teletask2MqttConfiguration;
import io.github.ridiekel.jeletask.mqtt.TeletaskService;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfig;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.types.*;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MqttProcessor implements StateChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(MqttProcessor.class);
    public static final int TIMEOUT = 30;

    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-zA-Z0-9_-]");
    private static final Map<String, String> WHAT_LOG_PATTERNS = Map.of(
            "EVENT", whatPattern(AnsiColor.YELLOW),
            "COMMAND", whatPattern(AnsiColor.MAGENTA),
            "CREATE", whatPattern(AnsiColor.CYAN),
            "DELETE", whatPattern(AnsiColor.BLUE)
    );

    private static String whatPattern(AnsiColor color) {
        return AnsiOutput.toString(color, "[%s] - %s", AnsiColor.DEFAULT, " - %s");
    }

    private final TeletaskClient teletaskClient;

    private MqttClient client;
    private final String prefix;
    private final TeletaskService service;
    private final MqttConnectOptions connOpts;
    private final String teletaskIdentifier;

    private final MotorProgressor motorProgressor;
    private final LongPressInputCaptor longPressInputCaptor;

    public MqttProcessor(TeletaskService service) {
        this.service = service;
        this.motorProgressor = new MotorProgressor(this);
        this.longPressInputCaptor = new LongPressInputCaptor(this);

        String host = service.getConfiguration().getMqtt().getHost();
        String port = Optional.ofNullable(service.getConfiguration().getMqtt().getPort()).orElse("1883");
        String username = Optional.ofNullable(service.getConfiguration().getMqtt().getUsername()).map(String::trim).filter(u -> !u.isEmpty()).orElse(null);
        char[] password = Optional.ofNullable(service.getConfiguration().getMqtt().getPassword()).map(String::toCharArray).orElse(null);
        String clientId = Optional.ofNullable(service.getConfiguration().getMqtt().getClientId()).orElse("teletask2mqtt");
        this.prefix = removeInvalid(service.getConfiguration().getMqtt().getPrefix(), clientId);

        this.teletaskClient = service.createClient(this);

        this.teletaskIdentifier = resolveTeletaskIdentifier(service, this.teletaskClient.getConfig());

        LOG.info(String.format("teletask id: '%s'", this.teletaskIdentifier));
        LOG.info(String.format("host: '%s'", host));
        LOG.info(String.format("port: '%s'", port));
        LOG.info(String.format("username: '%s'", Optional.ofNullable(username).orElse("<not specified>")));
        LOG.info(String.format("clientId: '%s'", clientId));
        LOG.info(String.format("retained: '%s'", this.service.getConfiguration().getMqtt().isRetained()));
        LOG.info(String.format("prefix: '%s'", this.prefix));
        LOG.info(String.format("Remote stop topic: '%s'", this.remoteStopTopic()));
        LOG.info(String.format("Remote refresh states topic: '%s'", this.remoteRefreshTopic()));

        this.connOpts = new MqttConnectOptions();
        this.connOpts.setMaxInflight(100000);

        // TODO:
        // Do we need CleanSession?
        // I have disabled it for now because it's not subscribing to the mqtt topics again
        // after a reconnect (for ex. when you restart your mqtt broker).
        this.connOpts.setCleanSession(false);

        this.connOpts.setAutomaticReconnect(true);
        this.connOpts.setUserName(username);
        this.connOpts.setPassword(password);

        this.connect(clientId, host, port);

        new Timer("motor-service").schedule(motorProgressor, 0, service.getConfiguration().getPublish().getMotorPositionInterval());

        scheduleGroupGet(service);
    }

    private void scheduleGroupGet(TeletaskService service) {
        if (service.getConfiguration().getPublish().getStatesInterval() > 0) {
            LOG.info(String.format("Scheduling force refresh every %s second(s)", service.getConfiguration().getPublish().getStatesInterval()));
            long intervalMillis = TimeUnit.SECONDS.toMillis(service.getConfiguration().getPublish().getStatesInterval());
            new Timer("groupget-service").schedule(new TimerTask() {
                @Override
                public void run() {
                    LOG.info("Forcing refresh of states...");
                    teletaskClient.groupGet();
                }
            }, intervalMillis, intervalMillis);
        }
    }

    public Teletask2MqttConfiguration getConfiguration() {
        return this.service.getConfiguration();
    }

    private static String resolveTeletaskIdentifier(TeletaskService service, CentralUnit centralUnit) {
        return removeInvalid(service.getConfiguration().getId(), removeInvalid(centralUnit.getHost() + "_" + centralUnit.getPort(), "impossible"));
    }

    private static String removeInvalid(String value, String defaultValue) {
        return Optional.ofNullable(value).map(String::trim).filter(u -> !u.isEmpty()).map(u -> INVALID_CHARS.matcher(u).replaceAll("_")).orElseGet(() -> removeInvalid(defaultValue, "<not_found>"));
    }

    private void connect(String clientId, String host, String port) {
        String broker = "tcp://" + host + ":" + port;
        LOG.info("Connecting to MQTT broker...");

        try {
            this.client = new MqttClient(broker, clientId, new MemoryPersistence());
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
        Awaitility.await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> {
            this.connect();
            this.publishConfig();
            this.subscribe();
            return true;
        });
    }

    private void subscribe() throws MqttException {
        LOG.info("Subscribing to topics...");
        this.client.subscribe(this.prefix + "/" + this.teletaskIdentifier + "/+/+/set", 0, new ChangeStateMqttCallback());
        this.client.subscribe(remoteStopTopic(), 0, (topic, message) -> {
            LOG.debug("Stopping the service: {}", Optional.ofNullable(message).map(MqttMessage::toString).orElse("<no reason provided>"));
            System.exit(0);
        });
        this.client.subscribe(remoteRefreshTopic(), 0, (topic, message) -> {
            LOG.debug("Refreshing states: {}", Optional.ofNullable(message).map(MqttMessage::toString).orElse("<no reason provided>"));
            this.teletaskClient.groupGet();
        });
    }

    private String remoteStopTopic() {
        return this.prefix + "/" + this.teletaskIdentifier + "/stop";
    }

    private String remoteRefreshTopic() {
        return this.prefix + "/" + this.teletaskIdentifier + "/refresh";
    }

    private synchronized void connect() {
        if (!this.client.isConnected()) {
            try {
                this.client.connect(this.connOpts);
            } catch (MqttException e) {
                LOG.debug("Connect warning: {}", e.getMessage());
            }

            Awaitility.await().pollDelay(100, TimeUnit.MILLISECONDS).atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> {
                LOG.info("Waiting for mqtt connection...");
                return this.client.isConnected();
            });
        }
    }

    private void publishConfig() {
        LOG.info("Publishing config...");
        this.teletaskClient.getConfig().getAllComponents().forEach(c -> {
            this.toConfig(c).forEach((t,m) -> {
                this.publish("CREATE", c, t, m, this.service.getConfiguration().getLog().isHaconfigEnabled() ? LOG::info : LOG::debug);
            });
        });
    }

    private Map<String, String> toConfig(ComponentSpec component) {
        return Optional.ofNullable(component)
                .flatMap(c -> Optional.ofNullable(FUNCTION_TO_TYPE.get(c.getFunction()))
                        .map(f -> f.getConfigTopicsAndMessages(this.teletaskClient.getConfig(), c, this.baseTopic(c), this.teletaskIdentifier, this.haDiscoveryPrefix()))
                ).orElse(new HashMap<>());
    }

    private String haDiscoveryPrefix() {
        return Optional.ofNullable(this.service.getConfiguration().getMqtt().getDiscoveryPrefix()).orElse("homeassistant");
    }

    private static final Map<Function, FunctionConfig> FUNCTION_TO_TYPE = Map.ofEntries(
            // COND and INPUT are readonly -> HA autodiscovery: binary_sensor
            Map.entry(Function.COND, f("binary_sensor", HABinarySensorConfig::new)),
            Map.entry(Function.INPUT, f("sensor", HAInputTriggerConfig::new)),
            // Dimmers -> -> HA auto discovery: light
            Map.entry(Function.DIMMER, f("light", HADimmerConfig::new)),
            // Flags can be read + turned on/off -> HA auto discovery: switch
            Map.entry(Function.FLAG, f("switch", HARelayConfig::new)),
            // Mood functions actually act like a switch in Teletask. They can be turned ON/OFF?
            // HA scenes can only be 'activated' and do not support a state?
            // -> HA auto discovery: switch
            Map.entry(Function.GENMOOD, f("switch", HARelayConfig::new)),
            Map.entry(Function.LOCMOOD, f("switch", HARelayConfig::new)),
            Map.entry(Function.TIMEDMOOD, f("switch", HARelayConfig::new)),
            // Motors -> HA auto discovery: cover. Sufficient?
            Map.entry(Function.MOTOR, f("cover", HAMotorConfig::new)),
            Map.entry(Function.SENSOR, f("sensor", HASensorConfig::new)),
            Map.entry(Function.RELAY, f("light", HARelayConfig::new)),
            // Timed functions actually act like a switch. They can only be ON or OFF -> HA auto discovery: switch
            Map.entry(Function.TIMEDFNC, f("switch", HARelayConfig::new))
    );

    static FunctionConfig f(String type, java.util.function.Function<HAConfigParameters, HAConfig<?>> config) {
        return new FunctionConfig(type, config);
    }

    private static class FunctionConfig {
        private final java.util.function.Function<ComponentSpec, String> type;
        private List<java.util.function.Function<HAConfigParameters, HAConfig<?>>> config;

        private FunctionConfig(String typeIfAbsent) {
            this.type = c -> Optional.ofNullable(c.getHAType()).orElse(typeIfAbsent);
            this.config = new ArrayList<>();
        }

        private FunctionConfig(String typeIfAbsent, java.util.function.Function<HAConfigParameters, HAConfig<?>> config) {
            this(typeIfAbsent);
            this.config = List.of(config);
        }

        protected String getHAType(ComponentSpec componentSpec, HAConfig<?> config) {
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
                String topic = createConfigTopic(componentSpec, haConfig, haNodeId, haDiscoveryPrefix);
                String message = Optional.ofNullable(haConfig).map(HAConfig::toString).orElse(null);
                topics.put(topic, message);
            }

            return topics;
        }

        private String createConfigTopic(ComponentSpec c, HAConfig<?> config, String haNodeId, String haDiscoveryPrefix) {
            //<discovery_prefix>/<component>/[<node_id>/]<object_id>/config
            return String.format("%s/%s/%s/%s/config",
                    haDiscoveryPrefix,
                    haComponent(c, config),
                    haNodeId,
                    haObjectId(c, config)
            );
        }

        protected String haObjectId(ComponentSpec c, HAConfig<?> config) {
            return c.getFunction().toString().toLowerCase() + "_" + c.getNumber();
        }

        protected String haComponent(ComponentSpec c, HAConfig<?> config) {
            return Optional.ofNullable(FUNCTION_TO_TYPE.get(c.getFunction())).map(f -> f.getHAType(c, config)).orElse("light");
        }
    }

    @Override
    public void receive(List<ComponentSpec> components) {
        components.forEach(c -> {
            publishState(c, c.getState());

            if (c.getFunction() == Function.MOTOR) {
                this.motorProgressor.update(c);
            } else if (c.getFunction() == Function.INPUT) {
                this.longPressInputCaptor.update(c);
            }
        });
    }

    public void publishState(ComponentSpec componentSpec, ComponentState state) {
        String ttTopic = this.baseTopic(componentSpec) + "/state";
        this.publish("EVENT", componentSpec, ttTopic, state.toString(), LOG::info);
    }

    private void publish(String what, ComponentSpec componentSpec, String topic, String message, Consumer<String> level) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(0);
            mqttMessage.setRetained(this.service.getConfiguration().getMqtt().isRetained());

            this.connect();

            LOG.debug(String.format("[%s] - %s - publishing topic '%s' -> %s", getWhat(what), getLoggingStringForComponent(componentSpec), topic, message));
            level.accept(String.format(WHAT_LOG_PATTERNS.get(what), getWhat(what), getLoggingStringForComponent(componentSpec), topicToLogWithColors(topic) + payloadToLogWithColors(message)));
            this.client.publish(topic, mqttMessage);
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    private String getLoggingStringForComponent(ComponentSpec componentSpec) {
        return String.format("[%s] - [%s] - [%s]", StringUtils.rightPad(componentSpec.getFunction().toString(), 10), StringUtils.leftPad(String.valueOf(componentSpec.getNumber()), 3), StringUtils.leftPad(componentSpec.getDescription(), 40));
    }

    private String getWhat(String what) {
        return StringUtils.rightPad(what, 7);
    }

    private String baseTopic(ComponentSpec c) {
        return this.prefix + "/" + this.teletaskIdentifier + "/" + c.getFunction().toString().toLowerCase() + "/" + c.getNumber();
    }

    @Override
    public void stop() {
        try {
            this.client.disconnect();
        } catch (MqttException e) {
            LOG.debug(e.getMessage());
        }
    }

    private class ChangeStateMqttCallback implements IMqttMessageListener {
        private final Pattern teletaskComponentPattern;

        ChangeStateMqttCallback() {
            this.teletaskComponentPattern = Pattern.compile(MqttProcessor.this.prefix + "/" + MqttProcessor.this.teletaskIdentifier + "/(\\w*)/(\\d*)/set");
        }

        @Override
        public void messageArrived(String topic, MqttMessage mqttMessage) {
            LOG.debug(String.format("MQTT message arrived '%s': '%s'", topic, new String(mqttMessage.getPayload())));
            String message = mqttMessage.toString();
            try {
                Matcher matcher = this.teletaskComponentPattern.matcher(topic);
                if (matcher.find()) {
                    Function function = Function.valueOf(matcher.group(1).toUpperCase());
                    int number = Integer.parseInt(matcher.group(2));
                    ComponentState state = ComponentState.parse(function, message);

                    String componentLog = getLoggingStringForComponent(MqttProcessor.this.teletaskClient.getConfig().getComponent(function, number));
                    LOG.info(String.format(WHAT_LOG_PATTERNS.get("COMMAND"), getWhat("COMMAND"), componentLog, payloadToLogWithColors(new String(mqttMessage.getPayload()))));

                    if (function == Function.DISPLAYMESSAGE)
                        MqttProcessor.this.teletaskClient.displaymessage(number, state,
                                (f, n, s) -> LOG.debug(String.format("[%s] MQTT topic '%s' changed state for: %s / %s -> %s", componentLog, topic, f, n, s)),
                                (f, n, s, e) -> LOG.warn(String.format("[%s] MQTT topic '%s' could not change state for: %s / %s -> %s", componentLog, topic, f, n, s)));

                    else
                        MqttProcessor.this.teletaskClient.set(function, number, state,
                                (f, n, s) -> LOG.debug(String.format("[%s] MQTT topic '%s' changed state for: %s / %s -> %s", componentLog, topic, f, n, s)),
                                (f, n, s, e) -> LOG.warn(String.format("[%s] MQTT topic '%s' could not change state for: %s / %s -> %s", componentLog, topic, f, n, s)));

                }
            } catch (Exception e) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(String.format("MQTT topic '%s' could not change state to: %s", topic, message), e);
                } else {
                    LOG.warn(String.format("MQTT topic '%s' could not change state to: %s -- %s", topic, message, e.getMessage()), e);
                }
            }
        }
    }

    private String payloadToLogWithColors(String payload) {
        return AnsiOutput.toString(PAYLOAD_LOG_COLORS.entrySet().stream().filter(e -> payload.contains(e.getKey())).findFirst().map(Map.Entry::getValue).orElse(AnsiColor.DEFAULT), payload, AnsiColor.DEFAULT);
    }

    private String topicToLogWithColors(String topic) {
        return this.service.getConfiguration().getLog().isTopicEnabled() ? AnsiOutput.toString(AnsiColor.MAGENTA, "[" + StringUtils.rightPad(topic, 60) + "] - ", AnsiColor.DEFAULT) : "";
    }

    private static final Map<String, AnsiColor> PAYLOAD_LOG_COLORS = new LinkedHashMap<>();

    static {
        PAYLOAD_LOG_COLORS.put("OPEN", AnsiColor.GREEN);
        PAYLOAD_LOG_COLORS.put("UP", AnsiColor.GREEN);
        PAYLOAD_LOG_COLORS.put("DOWN", AnsiColor.GREEN);
        PAYLOAD_LOG_COLORS.put("STOP", AnsiColor.RED);
        PAYLOAD_LOG_COLORS.put("ON", AnsiColor.GREEN);
        PAYLOAD_LOG_COLORS.put("OFF", AnsiColor.RED);
        PAYLOAD_LOG_COLORS.put("CLOSED", AnsiColor.RED);
        PAYLOAD_LOG_COLORS.put("0", AnsiColor.RED);
        for (int i = 1; i <= 100; i++) {
            PAYLOAD_LOG_COLORS.put(String.valueOf(i), AnsiColor.GREEN);
        }
    }
}
