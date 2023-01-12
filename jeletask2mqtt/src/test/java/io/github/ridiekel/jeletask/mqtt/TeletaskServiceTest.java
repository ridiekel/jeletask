package io.github.ridiekel.jeletask.mqtt;

import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessor;
import io.github.ridiekel.jeletask.server.TeletaskTestServer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

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

    private static final Path HA_CONFIG_DIR = createHaConfigDir();

    private static final GenericContainer HOME_ASSISTANT = new GenericContainer(DockerImageName.parse("ghcr.io/home-assistant/home-assistant:stable"))
            .withExposedPorts(8123)
            .withFileSystemBind(HA_CONFIG_DIR.toString(), "/config", BindMode.READ_WRITE)
            .withNetwork(MQTT.getNetwork());

    static {
        MQTT.start();

        HOME_ASSISTANT.start();
    }

    private static Path createHaConfigDir() {
        Path configDir;
        try {
            configDir = Files.createTempDirectory("jeletask-ha");

            System.out.println("******************* configDir = " + configDir);

            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("/haconfig/**");
            for (Resource resource : resources) {
                if (resource.exists() & resource.isReadable()) {
                    URL url = resource.getURL();
                    String urlString = url.toExternalForm();
                    String targetName = urlString.substring(urlString.indexOf("haconfig"));
                    Path destination = configDir.resolve(targetName);
                    Files.createDirectories(destination.getParent());
                    Files.copy(url.openStream(), destination);
                    System.out.println("Copied " + url + " to " + destination.toAbsolutePath());
                } else {
//                    System.out.println("Did not copy, seems to be directory: " + resource.getDescription());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        return configDir.resolve("haconfig").toAbsolutePath();
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
    void one() throws InterruptedException, IOException {
        this.server.mock(e -> {
                    e.when(set(Function.RELAY, 1, "ON")).thenRespond(state(Function.RELAY, 1, "ON"));
                    e.when(get(Function.RELAY, 1)).thenRespond(state(Function.RELAY, 1, "ON"));
                    e.when(groupGet(Function.RELAY, 1, 2)).thenRespond(
                            state(Function.RELAY, 1, "ON"),
                            state(Function.RELAY, 2, "OFF")
                    );
                }
        );
//
        System.out.println("******** HA " + HOME_ASSISTANT.getFirstMappedPort());
        System.out.println("******** MQTT " + MQTT.getFirstMappedPort());
        WebClient client = WebClient.builder()
                .baseUrl("http://localhost:"+HOME_ASSISTANT.getFirstMappedPort()+"/api")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJiZmQ2ZTAzNjMyNDk0YWJmYmVlN2ViMDdmYTgyZDM3ZCIsImlhdCI6MTY3MzUzMjY5MSwiZXhwIjoxOTg4ODkyNjkxfQ.dVwwMYpmTiTHmN5LqS3apU2mbmwtml5gPzvgaDTWikQ")
                .build();

        Mono<String> stringMono = client.get().uri("/states").retrieve().bodyToMono(String.class);
        System.out.println(stringMono.block());

        mqttProcessor.publishConfig();

//        Runtime.getRuntime().exec("xdg-open http://localhost:"+HOME_ASSISTANT.getFirstMappedPort());
//        Thread.sleep(Long.MAX_VALUE);
    }
}

