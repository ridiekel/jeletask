package io.github.ridiekel.jeletask.mqtt.container.mqtt;

import io.github.ridiekel.jeletask.mqtt.Teletask2MqttConfiguration;
import io.github.ridiekel.jeletask.mqtt.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.PostConstruct;

@Service
public class MqttContainer {
    private final GenericContainer container;
    private final Teletask2MqttConfigurationProperties properties;

    public MqttContainer(Teletask2MqttConfigurationProperties properties) {
        this.properties = properties;
        container = new GenericContainer(DockerImageName.parse("eclipse-mosquitto:latest"))
                .withExposedPorts(1883)
                .withNetworkAliases("mqtt")
                .withCommand("mosquitto -c /mosquitto-no-auth.conf")
                .withNetwork(Network.newNetwork());
    }

    @EventListener(classes = { ContextRefreshedEvent.class })
    @Order(100)
    public void start() {
        container.start();
        this.properties.getMqtt().setPort(String.valueOf(getPort()));
    }

    public Integer getPort() {
        return container.getFirstMappedPort();
    }

    public Network getNetwork() {
        return container.getNetwork();
    }
}
