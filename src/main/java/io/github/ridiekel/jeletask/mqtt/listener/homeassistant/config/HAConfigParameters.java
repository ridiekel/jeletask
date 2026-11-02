package io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config;

import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HomeAssistentAutoConfig;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.types.HASensorConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter
@Setter
public class HAConfigParameters {
    private final Teletask2MqttConfigurationProperties config;
    private final CentralUnit centralUnit;
    private final ComponentSpec componentSpec;
    private final HomeAssistentAutoConfig.HADeviceType deviceType;
    private final HASensorConfig viaDevice;
    private final String componentTopic;
    private final String availabilityTopic;
    private final String identifier;

    public HAConfigParameters(Teletask2MqttConfigurationProperties config, CentralUnit centralUnit, ComponentSpec componentSpec, String baseTopic, String identifier, HomeAssistentAutoConfig.HADeviceType deviceType, HASensorConfig viaDevice) {
        this.config = config;
        this.centralUnit = centralUnit;
        this.componentSpec = componentSpec;
        this.deviceType = deviceType;
        this.viaDevice = viaDevice;
        this.componentTopic = componentTopic(baseTopic, componentSpec);
        this.identifier = identifier;
        this.availabilityTopic = availabilityTopic(baseTopic);
    }

    public Optional<HASensorConfig> getViaDevice() {
        return Optional.ofNullable(viaDevice);
    }

    public static String componentTopic(String baseTopic, ComponentSpec c) {
        return baseTopic + "/" + c.getFunction().toString().toLowerCase() + "/" + c.getNumber();
    }

    public static String availabilityTopic(String baseTopic) {
        return baseTopic + "/state";
    }
}
