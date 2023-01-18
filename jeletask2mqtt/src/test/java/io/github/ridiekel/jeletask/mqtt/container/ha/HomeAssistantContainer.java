package io.github.ridiekel.jeletask.mqtt.container.ha;

import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.mqtt.container.mqtt.MqttContainer;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectWriter;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Service
public class HomeAssistantContainer extends GenericContainer<HomeAssistantContainer> {
    private static final Logger LOGGER = LogManager.getLogger();

    private WebClient haWebClient;
    private final MqttProcessor mqttProcessor;
    private final MqttContainer mqttContainer;

    public HomeAssistantContainer(MqttProcessor mqttProcessor, MqttContainer mqttContainer) {
        super(DockerImageName.parse("ghcr.io/home-assistant/home-assistant:stable"));
        this.mqttProcessor = mqttProcessor;
        this.mqttContainer = mqttContainer;

        this.withExposedPorts(8123)
                .withNetwork(mqttContainer.getNetwork());
    }

    @EventListener(classes = {ContextRefreshedEvent.class})
    @Order(300)
    public void start() {
        LOGGER.info(AnsiOutput.toString(AnsiColor.BRIGHT_MAGENTA, "Starting Home Assistant", AnsiColor.DEFAULT));

        Path configDir = prepareTempHaConfigDir();

        this.withFileSystemBind(configDir.toString(), "/config", BindMode.READ_WRITE);

        super.start();

        this.haWebClient = WebClient.builder()
                .baseUrl("http://localhost:" + this.getFirstMappedPort() + "/api")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJiZmQ2ZTAzNjMyNDk0YWJmYmVlN2ViMDdmYTgyZDM3ZCIsImlhdCI6MTY3MzUzMjY5MSwiZXhwIjoxOTg4ODkyNjkxfQ.dVwwMYpmTiTHmN5LqS3apU2mbmwtml5gPzvgaDTWikQ")
                .build();

        AtomicBoolean haOnline = new AtomicBoolean(false);
        this.mqttContainer.subscribe("homeassistant/status", (t, m) -> {
            if (Objects.equals(new String(m.getPayload()), "online")) {
                haOnline.set(true);
            }
        });

        Awaitility.await("Home Assistent Started").pollDelay(1, TimeUnit.SECONDS).atMost(1, TimeUnit.MINUTES).until(haOnline::get);


        org.awaitility.Awaitility.await("Home Assistent Teletask Config Published")
                .atMost(10, TimeUnit.SECONDS)
                .pollDelay(1, TimeUnit.SECONDS)
                .until(() -> {
                    mqttProcessor.publishConfig();
                    return this.states().size() > 10;
                });

        LOGGER.info(AnsiOutput.toString(AnsiColor.BRIGHT_GREEN, "Home Assistant startup complete", AnsiColor.DEFAULT));

        this.mqttContainer.startCapturing();
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
        return this.getFirstMappedPort();
    }

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();

    public Entity state(String id) {
        return haWebClient.get().uri("/states/" + id).retrieve().toEntity(Entity.class).block().getBody();
    }

    public Entity state(Function function, int number, String type) {
        return state(type + ".teletask_man_test_localhost_1234_" + function.toString().toLowerCase() + "_" + number);
    }

    public List<Entity> states() {
        return haWebClient.get().uri("/states").retrieve().toEntityList(Entity.class).block().getBody();
    }

    public String statesString() {
        return haWebClient.get().uri("/states").retrieve().bodyToMono(String.class).block();
    }

    public void openBrowser() {
        try {
            Runtime.getRuntime().exec("xdg-open http://localhost:" + this.getPort());
            Thread.sleep(Long.MAX_VALUE);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public EntityExpectationBuilder expectEntity(Function function, int number, String type) {
        return new EntityExpectationBuilder(() -> this.state(function, number, type));
    }

    public static class EntityExpectationBuilder {
        private final Supplier<Entity> entity;

        public EntityExpectationBuilder(Supplier<Entity> entity) {
            this.entity = entity;
        }

        public void toHaveState(String state) {
            this.toMatch("state: '" + state + "'", e -> Objects.equals(e.getState(), state));
        }

        public void toMatch(String describe, Predicate<Entity> matcher) {
            Entity entity = this.entity.get();

            String message = AnsiOutput.toString("[%s]", AnsiColor.DEFAULT, " Expectation for entity '", AnsiColor.BRIGHT_CYAN, entity.getEntity_id(), AnsiColor.DEFAULT, "': ", AnsiColor.BRIGHT_YELLOW, describe, AnsiColor.DEFAULT);

            Awaitility.await(AnsiOutput.toString(AnsiColor.BRIGHT_RED, String.format(message, "FAILED")))
                    .pollInterval(250, TimeUnit.MILLISECONDS)
                    .atMost(10, TimeUnit.SECONDS)
                    .until(() -> matcher.test(entity));

            LOGGER.info(AnsiOutput.toString(AnsiColor.BRIGHT_GREEN, String.format(message, "SUCCESS")));
        }
    }
}
