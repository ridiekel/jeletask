package io.github.ridiekel.jeletask.mqtt.container;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestContainers {


    private static final HomeAssistant HOME_ASSISTANT = new HomeAssistant();
    private static final Mqtt MQTT = new Mqtt();

    public static HomeAssistant ha() {
        return HOME_ASSISTANT;
    }

    public static Mqtt mqtt() {
        return MQTT;
    }

    public static class HomeAssistant {
        private static final Path HA_CONFIG_DIR = createHaConfigDir();

        private static final GenericContainer CONTAINER = new GenericContainer(DockerImageName.parse("ghcr.io/home-assistant/home-assistant:stable"))
                .withExposedPorts(8123)
                .withFileSystemBind(HA_CONFIG_DIR.toString(), "/config", BindMode.READ_WRITE)
                .withNetwork(Mqtt.CONTAINER.getNetwork());

        static {
            CONTAINER.start();
        }

        private static Path createHaConfigDir() {
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

        private final WebClient HA_CLIENT = WebClient.builder()
                .baseUrl("http://localhost:"+ CONTAINER.getFirstMappedPort()+"/api")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJiZmQ2ZTAzNjMyNDk0YWJmYmVlN2ViMDdmYTgyZDM3ZCIsImlhdCI6MTY3MzUzMjY5MSwiZXhwIjoxOTg4ODkyNjkxfQ.dVwwMYpmTiTHmN5LqS3apU2mbmwtml5gPzvgaDTWikQ")
                .build();

        public Integer getPort() {
            return CONTAINER.getFirstMappedPort();
        }

        public String states() {
            return HA_CLIENT.get().uri("/states").retrieve().bodyToMono(String.class).block();
        }
    }

    public static class Mqtt {
        private static final GenericContainer CONTAINER = new GenericContainer(DockerImageName.parse("eclipse-mosquitto:latest"))
                .withExposedPorts(1883)
                .withNetworkAliases("mqtt")
                .withCommand("mosquitto -c /mosquitto-no-auth.conf")
                .withNetwork(Network.newNetwork());

        static {
            CONTAINER.start();
        }

        public Integer getPort() {
            return CONTAINER.getFirstMappedPort();
        }
    }
}
