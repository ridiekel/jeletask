package io.github.ridiekel.jeletask.mqtt.container.mqtt;

import org.springframework.stereotype.Service;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

@Service
public class MqttContainer {
    private final GenericContainer container;

    public MqttContainer() {
        container = new GenericContainer(DockerImageName.parse("eclipse-mosquitto:latest"))
                .withExposedPorts(1883)
                .withNetworkAliases("mqtt")
                .withCommand("mosquitto -c /mosquitto-no-auth.conf")
                .withNetwork(Network.newNetwork());
        container.start();
    }

    public Integer getPort() {
        return container.getFirstMappedPort();
    }

    public Network getNetwork() {
        return container.getNetwork();
    }
}
