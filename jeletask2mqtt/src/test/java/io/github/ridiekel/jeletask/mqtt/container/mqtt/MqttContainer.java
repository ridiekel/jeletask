package io.github.ridiekel.jeletask.mqtt.container.mqtt;

import io.github.ridiekel.jeletask.mqtt.Teletask2MqttConfigurationProperties;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

@Service
public class MqttContainer extends GenericContainer<MqttContainer> {
    private final Teletask2MqttConfigurationProperties properties;

    public MqttContainer(Teletask2MqttConfigurationProperties properties) {
        super(DockerImageName.parse("eclipse-mosquitto:latest"));
        this.properties = properties;
        this.withExposedPorts(1883)
                .withNetworkAliases("mqtt")
                .withCommand("mosquitto -c /mosquitto-no-auth.conf")
                .withNetwork(Network.newNetwork());
    }

    @EventListener(classes = { ContextRefreshedEvent.class })
    @Order(100)
    public void start() {
        super.start();
        this.properties.getMqtt().setPort(String.valueOf(getPort()));
    }

    public Integer getPort() {
        return this.getFirstMappedPort();
    }
}
