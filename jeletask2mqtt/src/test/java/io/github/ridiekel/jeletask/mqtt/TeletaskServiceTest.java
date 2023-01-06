package io.github.ridiekel.jeletask.mqtt;

import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessor;
import io.github.ridiekel.jeletask.server.TeletaskTestServer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import static io.github.ridiekel.jeletask.server.ExpectationBuilder.ExpectationResponseBuilder.state;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.get;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.groupGet;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.set;

@ActiveProfiles("test")
@SpringBootTest
class TeletaskServiceTest {
    private static final GenericContainer MQTT = new GenericContainer(DockerImageName.parse("eclipse-mosquitto:latest"))
            .withExposedPorts(1883)
            .withNetworkAliases("mqtt")
            .withCommand("mosquitto -c /mosquitto-no-auth.conf")
            .withNetwork(Network.newNetwork());

    private static final GenericContainer HOME_ASSISTANT = new GenericContainer(DockerImageName.parse("ghcr.io/home-assistant/home-assistant:stable"))
            .withClasspathResourceMapping("/haconfig", "/config", BindMode.READ_WRITE)
            .withExposedPorts(8123)
            .withNetwork(MQTT.getNetwork());

    static {
        MQTT.start();
        HOME_ASSISTANT.start();
    }

    @Autowired
    private TeletaskTestServer server;

    @Autowired
    private MqttProcessor mqttProcessor;

    @Autowired
    private TeletaskClient teletaskClient;


    @DynamicPropertySource
    public static void setupProperties(DynamicPropertyRegistry registry) {
        registry.add("teletask.mqtt.port", MQTT::getFirstMappedPort);
    }

    @Test
    void one() {
        this.server.mock(e -> {
                    e.when(set(Function.RELAY, 1, "ON")).thenRespond(state(Function.RELAY, 1, "ON"));
                    e.when(get(Function.RELAY, 1)).thenRespond(state(Function.RELAY, 1, "ON"));
                    e.when(groupGet(Function.RELAY, 1, 2)).thenRespond(
                            state(Function.RELAY, 1, "ON"),
                            state(Function.RELAY, 2, "OFF")
                    );
                }
        );

        System.out.println("******** HA " + HOME_ASSISTANT.getFirstMappedPort());
        System.out.println("******** MQTT " + MQTT.getFirstMappedPort());

        mqttProcessor.publishConfig();

        Awaitility.await().forever();
    }
}

