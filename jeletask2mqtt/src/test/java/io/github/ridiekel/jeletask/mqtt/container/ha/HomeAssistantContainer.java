package io.github.ridiekel.jeletask.mqtt.container.ha;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.testcontainers.utility.DockerImageName;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

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

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

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

    public String statesAsString() {
        return haWebClient.get().uri("/states").retrieve().bodyToMono(String.class).block();
    }

    public void openBrowser() {
        try {
            String url = "http://localhost:" + this.getPort();
            LOGGER.info(AnsiOutput.toString(AnsiColor.GREEN, "Starting browser and pointing it to: ", AnsiColor.BLUE, url, AnsiColor.DEFAULT));

            Desktop.getDesktop().browse(new URI(url));
            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HomeAssistantExpectationBuilder expect() {
        return new HomeAssistantExpectationBuilder(this);
    }

    public static class HomeAssistantExpectationBuilder {
        private final HomeAssistantContainer container;

        public HomeAssistantExpectationBuilder(HomeAssistantContainer container) {
            this.container = container;
        }

        public EntityExpectationBuilder relay(int number) {
            return this.function(Function.RELAY, number);
        }

        public EntityExpectationBuilder function(Function function, int number) {
            return new EntityExpectationBuilder(this, function, number);
        }

        public static class EntityExpectationBuilder {
            private final HomeAssistantExpectationBuilder homeAssistantBuilder;
            private final Function function;
            private final int number;

            public EntityExpectationBuilder(HomeAssistantExpectationBuilder homeAssistantBuilder, Function function, int number) {
                this.homeAssistantBuilder = homeAssistantBuilder;
                this.function = function;
                this.number = number;
            }

            public EntityTypeExpectationBuilder asLight() {
                return new EntityTypeExpectationBuilder(this, "light");
            }

            public static class EntityTypeExpectationBuilder {
                private final EntityExpectationBuilder entityBuilder;
                private final String type;

                public EntityTypeExpectationBuilder(EntityExpectationBuilder entityBuilder, String type) {
                    this.entityBuilder = entityBuilder;
                    this.type = type;
                }

                public EntityObjectExpectationBuilder toHave() {
                    return new EntityObjectExpectationBuilder(this);
                }

                public static class EntityObjectExpectationBuilder {
                    private final EntityTypeExpectationBuilder entityTypeBuilder;

                    public EntityObjectExpectationBuilder(EntityTypeExpectationBuilder entityTypeBuilder) {
                        this.entityTypeBuilder = entityTypeBuilder;
                    }

                    public EntityObjectStateExpectationBuilder state() {
                        return new EntityObjectStateExpectationBuilder(this);
                    }


                    public static class EntityObjectStateExpectationBuilder {
                        private final EntityObjectExpectationBuilder entityObjectBuilder;

                        public EntityObjectStateExpectationBuilder(EntityObjectExpectationBuilder entityObjectBuilder) {
                            this.entityObjectBuilder = entityObjectBuilder;
                        }

                        public void on() {
                            value("ON");
                        }

                        public void off() {
                            value("OFF");
                        }

                        public void value(String state) {
                            this.entityObjectBuilder.entityTypeBuilder.match("state: '" + state + "'", e -> Objects.equals(e.getState(), state));
                        }
                    }
                }

                public void match(String describe, Predicate<Entity> matcher) {
                    Entity entity = entityBuilder.homeAssistantBuilder.container.state(entityBuilder.function, entityBuilder.number, type);

                    String message = AnsiOutput.toString("[%s]", AnsiColor.DEFAULT, " Entity '", AnsiColor.BRIGHT_CYAN, entity.getEntity_id(), AnsiColor.DEFAULT, "' expected to have: ", AnsiColor.BRIGHT_YELLOW, describe, AnsiColor.DEFAULT);

                    try {
                        Awaitility.await(describe)
                                .pollInterval(250, TimeUnit.MILLISECONDS)
                                .atMost(10, TimeUnit.SECONDS)
                                .until(() -> matcher.test(entity));
                    } catch (Exception e) {
                        LOGGER.error(AnsiOutput.toString(AnsiColor.BRIGHT_RED, String.format(message, "FAILED"), " - but was: ", AnsiColor.RED, entity, AnsiColor.DEFAULT));
                        throw e;
                    }

                    LOGGER.info(AnsiOutput.toString(AnsiColor.BRIGHT_GREEN, String.format(message, "SUCCESS")));
                }
            }
        }
    }
}
