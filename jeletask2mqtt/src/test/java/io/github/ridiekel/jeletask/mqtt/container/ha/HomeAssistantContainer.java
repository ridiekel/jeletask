package io.github.ridiekel.jeletask.mqtt.container.ha;

import io.github.ridiekel.jeletask.mqtt.container.mqtt.MqttContainer;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessor;
import org.awaitility.Awaitility;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class HomeAssistantContainer {

    private final GenericContainer container;
    private final WebClient haWebClient;

    public HomeAssistantContainer(MqttProcessor mqttProcessor, MqttContainer mqttContainer) {
        Path configDir = prepareTempHaConfigDir();

        container = new GenericContainer(DockerImageName.parse("ghcr.io/home-assistant/home-assistant:stable"))
                .withExposedPorts(8123)
                .withFileSystemBind(configDir.toString(), "/config", BindMode.READ_WRITE)
                .withNetwork(mqttContainer.getNetwork());
        container.start();

        this.haWebClient = WebClient.builder()
                .baseUrl("http://localhost:" + container.getFirstMappedPort() + "/api")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJiZmQ2ZTAzNjMyNDk0YWJmYmVlN2ViMDdmYTgyZDM3ZCIsImlhdCI6MTY3MzUzMjY5MSwiZXhwIjoxOTg4ODkyNjkxfQ.dVwwMYpmTiTHmN5LqS3apU2mbmwtml5gPzvgaDTWikQ")
                .build();

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollDelay(1, TimeUnit.SECONDS)
                .until(() -> {
                    mqttProcessor.publishConfig();
                    return this.states().size() > 10;
                });
    }

    private Path prepareTempHaConfigDir() {
        Path configDir;
        try {
            configDir = Files.createTempDirectory("jeletask-ha");

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
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        return configDir.resolve("haconfig").toAbsolutePath();
    }

    public Integer getPort() {
        return container.getFirstMappedPort();
    }

    public List<Entity> states() {
        return haWebClient.get().uri("/states").retrieve().toEntityList(Entity.class).block().getBody();
    }

    public String statesString() {
        return haWebClient.get().uri("/states").retrieve().bodyToMono(String.class).block();
    }
}
