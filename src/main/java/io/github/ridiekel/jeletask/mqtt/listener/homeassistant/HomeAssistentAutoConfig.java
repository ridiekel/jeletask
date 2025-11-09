package io.github.ridiekel.jeletask.mqtt.listener.homeassistant;

import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.SensorType;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfig;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfigParameters;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.types.*;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public class HomeAssistentAutoConfig {
    private static final Logger LOG = LogManager.getLogger();
    private final Teletask2MqttConfigurationProperties configuration;
    private final CentralUnit centralUnit;
    private final String baseTopic;
    private final String teletaskIdentifier;

    public HomeAssistentAutoConfig(Teletask2MqttConfigurationProperties configuration, CentralUnit centralUnit, String baseTopic, String teletaskIdentifier) {
        this.configuration = configuration;
        this.centralUnit = centralUnit;
        this.baseTopic = baseTopic;
        this.teletaskIdentifier = teletaskIdentifier;
    }

    public HAConfig<?> toConfig(ComponentSpec component) {
        return Optional.ofNullable(component)
                .flatMap(c -> Optional.ofNullable(FUNCTION_TO_TYPE.get(c.getFunction()))
                        .map(f -> f.getHaConfig(this.configuration, this.centralUnit, c, this.baseTopic, this.teletaskIdentifier))
                ).orElse(null);
    }

    private static final Map<Function, FunctionConfig> FUNCTION_TO_TYPE = Map.ofEntries(
            // COND and INPUT are readonly -> HA autodiscovery: binary_sensor
            Map.entry(Function.COND, f(HADeviceType.BINARY_SENSOR, HABinarySensorConfig::new)),
            Map.entry(Function.INPUT, f(HADeviceType.SENSOR, HAInputTriggerConfig::new)),
            // Dimmers -> -> HA auto discovery: light
            Map.entry(Function.DIMMER, f(HADeviceType.LIGHT, HADimmerConfig::new)),
            // Flags can be read + turned on/off -> HA auto discovery: switch
            Map.entry(Function.FLAG, f(switchSceneLight(HADeviceType.SWITCH), HAOnOffConfig::new)),
            // Mood functions actually act like a switch in Teletask. They can be turned ON/OFF?
            // HA scenes can only be 'activated' and do not support a state?
            // -> HA auto discovery: switch
            Map.entry(Function.GENMOOD, f(switchSceneLight(HADeviceType.SWITCH), HAOnOffConfig::new)),
            Map.entry(Function.LOCMOOD, f(switchSceneLight(HADeviceType.SWITCH), HAOnOffConfig::new)),
            Map.entry(Function.TIMEDMOOD, f(switchSceneLight(HADeviceType.SWITCH), HAOnOffConfig::new)),
            // Motors -> HA auto discovery: cover. Sufficient?
            Map.entry(Function.MOTOR, f(HADeviceType.COVER, HAMotorConfig::new)),
            Map.entry(Function.SENSOR, f(haDeviceTypeFromTypeToHaTypeMap(Map.of(
                    SensorType.STRING, HADeviceType.SENSOR,
                    SensorType.TEMPERATURE, HADeviceType.SENSOR,
                    SensorType.LIGHT, HADeviceType.SENSOR,
                    SensorType.HUMIDITY, HADeviceType.SENSOR,
                    SensorType.GAS, HADeviceType.SENSOR,
                    SensorType.TEMPERATURECONTROL, HADeviceType.CLIMATE,
                    SensorType.PULSECOUNTER, HADeviceType.SENSOR
            )), HASensorConfig::new)),
            Map.entry(Function.RELAY, f(switchSceneLight(HADeviceType.LIGHT), HAOnOffConfig::new)),
            // Timed functions actually act like a switch. They can only be ON or OFF -> HA auto discovery: switch
            Map.entry(Function.TIMEDFNC, f(switchSceneLight(HADeviceType.SWITCH), HAOnOffConfig::new))
    );

    private static java.util.function.Function<ComponentSpec, HADeviceType> switchSceneLight(HADeviceType defaultType) {
        return c -> {
            HADeviceType haDeviceType = Optional.ofNullable(c.getType()).map(String::toUpperCase).map(HADeviceType::valueOf).orElse(defaultType);
            c.setType(haDeviceType.toString().toLowerCase());
            return haDeviceType;
        };
    }

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

    public enum HADeviceType {
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

    public static class FunctionConfig {

        private final java.util.function.Function<ComponentSpec, String> type;
        private final java.util.function.Function<HAConfigParameters, HAConfig<?>> config;

        private FunctionConfig(java.util.function.Function<ComponentSpec, HADeviceType> typeIfAbsent, java.util.function.Function<HAConfigParameters, HAConfig<?>> config) {
            this.type = c -> Optional.ofNullable(c.getHAType()).orElseGet(() -> typeIfAbsent.apply(c).toString());
            this.config = config;
        }

        protected String getHAType(ComponentSpec componentSpec) {
            return this.type.apply(componentSpec);
        }

        public HAConfig<?> getHaConfig(Teletask2MqttConfigurationProperties configuration, CentralUnit centralUnit, ComponentSpec componentSpec, String baseTopic, String teletaskIdentifier) {
            String configTopic = createConfigTopic(configuration, componentSpec, teletaskIdentifier);
            HAConfigParameters params = new HAConfigParameters(
                    configuration,
                    centralUnit,
                    componentSpec,
                    baseTopic,
                    configTopic,
                    teletaskIdentifier,
                    HADeviceType.valueOf(this.getHAType(componentSpec).toUpperCase()),
                    (HASensorConfig) centralUnit.getBridge().getHaPublishedConfig()
            );
            HAConfig<?> haConfig = this.config.apply(params);
            String message = Optional.ofNullable(haConfig).map(HAConfig::toString).orElse(null);
            haConfig.setMqttConfigTopic(new HAConfig.MQTTConfigTopic(configTopic, message));
            componentSpec.setHaPublishedConfig(haConfig);
            return haConfig;
        }

        protected String haObjectId(ComponentSpec c) {
            return c.getFunction().toString().toLowerCase() + "_" + c.getNumber();
        }

        private String createConfigTopic(Teletask2MqttConfigurationProperties configuration, ComponentSpec c, String haNodeId) {
            //<discovery_prefix>/<component>/[<node_id>/]<object_id>/config
            return String.format("%s/%s/%s/%s/config",
                    Optional.ofNullable(configuration.getMqtt().getDiscoveryPrefix()).orElse("homeassistant"),
                    haComponent(c),
                    haNodeId,
                    haObjectId(c)
            );
        }

        protected String haComponent(ComponentSpec c) {
            return Optional.ofNullable(c.getHAType()).orElse(FUNCTION_TO_TYPE.get(c.getFunction()).getHAType(c));
        }
    }

}
