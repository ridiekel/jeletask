package io.github.ridiekel.jeletask.mqtt.container;

import io.github.ridiekel.jeletask.mqtt.container.ha.HomeAssistantContainer;
import io.github.ridiekel.jeletask.mqtt.container.mqtt.MqttContainer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class TestContainers {


    private final HomeAssistantContainer ha;
    private final MqttContainer mqtt;

    public TestContainers(HomeAssistantContainer ha, MqttContainer mqtt) {
        this.ha = ha;
        this.mqtt = mqtt;
    }

    public HomeAssistantContainer ha() {
        return ha;
    }

    public MqttContainer mqtt() {
        return mqtt;
    }
}
