package io.github.ridiekel.jeletask.mqtt.container.ha;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.mqtt.container.RedirectingSlf4jLogConsumer;
import io.github.ridiekel.jeletask.mqtt.container.mqtt.MqttContainer;
import io.github.ridiekel.jeletask.utilities.StringUtilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

@Service
public class HomeAssistantContainer extends GenericContainer<HomeAssistantContainer> {

    private static final Logger LOG = LogManager.getLogger();

    private static final String BEARER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJiZmQ2ZTAzNjMyNDk0YWJmYmVlN2ViMDdmYTgyZDM3ZCIsImlhdCI6MTY3MzUzMjY5MSwiZXhwIjoxOTg4ODkyNjkxfQ.dVwwMYpmTiTHmN5LqS3apU2mbmwtml5gPzvgaDTWikQ";

    private String apiUrl;

    private WebClient haWebClient;
    private final MqttContainer mqttContainer;
    private final Teletask2MqttConfigurationProperties configuration;
    private final CentralUnit centralUnit;

    public HomeAssistantContainer(MqttContainer mqttContainer, Teletask2MqttConfigurationProperties configuration, CentralUnit centralUnit) {
        super(new ImageFromDockerfile()
                .withDockerfile(Paths.get("src/test/resources/haconfig/Dockerfile")));
        this.mqttContainer = mqttContainer;
        this.configuration = configuration;
        this.centralUnit = centralUnit;

        this.withExposedPorts(8123)
                .withStartupTimeout(Duration.of(5, ChronoUnit.MINUTES))
                .withNetwork(mqttContainer.getNetwork());
    }

    private static boolean started = false;

    public static void waitForHaReady(Duration timeout) {
        WebDriver driver = WebDriverRunner.getWebDriver();
        WebDriverWait wait = new WebDriverWait(driver, timeout);

        wait.until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));

        $("home-assistant").should(exist);

        wait.until(d -> (Boolean) ((JavascriptExecutor) d).executeScript(
                "if (window.__haWsReady === true) return true;" +
                        "if (window.hassConnection && typeof window.hassConnection.then === 'function') {" +
                        "  window.hassConnection.then(() => window.__haWsReady = true);" +
                        "}" +
                        "return window.__haWsReady === true;"
        ));
    }

    public static void waitForPossibleReloadAndStability(Duration totalTimeout, Duration quietPeriod) {
        WebDriver driver = WebDriverRunner.getWebDriver();
        WebDriverWait wait = new WebDriverWait(driver, totalTimeout);

        SelenideElement root = $("home-assistant").should(exist);
        WebElement rootEl = root.getWrappedElement();

        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.stalenessOf(rootEl));
            waitForHaReady(Duration.ofSeconds(30));
        } catch (TimeoutException ignore) {
        }

        wait.until(d -> {
            try {
                WebElement el = $("home-assistant").getWrappedElement();
                Selenide.sleep((int) quietPeriod.toMillis());
                el.isEnabled();
                return true;
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    public void login() {
        if (!started) {
            String url = "http://" + this.getHost() + ":" + this.getPort();
            open(url);
            waitForHaReady(Duration.ofSeconds(30));
            waitForPossibleReloadAndStability(Duration.ofSeconds(20), Duration.ofMillis(800));
            this.mqttContainer.startCapturing();
            started = true;
        }
    }

    @EventListener(classes = {ContextRefreshedEvent.class})
    @Order(300)
    public void start() {
        LOG.info(AnsiOutput.toString(AnsiColor.BRIGHT_MAGENTA, "Starting Home Assistant", AnsiColor.DEFAULT));

        AtomicBoolean haOnline = new AtomicBoolean(false);
        String topicFilter = "homeassistant/status";
        this.mqttContainer.subscribe(topicFilter, (t, m) -> {
            String payload = new String(m.getPayload());
            if (Objects.equals(payload, "online")) {
                haOnline.set(true);
            }
        });

        super.start();

        this.followOutput(new RedirectingSlf4jLogConsumer(this.getClass(), AnsiColor.MAGENTA, "ha-container-log"));

        String baseUrl = "http://" + this.getHost() + ":" + this.getFirstMappedPort();
        apiUrl = baseUrl + "/api";

        LOG.info(AnsiOutput.toString(AnsiColor.BRIGHT_MAGENTA, "Home Assistent expected to start on address: ", AnsiColor.BRIGHT_WHITE, baseUrl, AnsiColor.DEFAULT));

        this.haWebClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + BEARER)
                .build();

        Awaitility.await("Home Assistent Started")
                .atMost(5, TimeUnit.MINUTES)
                .pollDelay(1, TimeUnit.SECONDS)
                .until(haOnline::get);

        LOG.info(AnsiOutput.toString(AnsiColor.BRIGHT_MAGENTA, "Home Assistant online according to MQTT", AnsiColor.DEFAULT));

        org.awaitility.Awaitility.await("Home Assistent Teletask Config Published")
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> this.states().size() >= this.centralUnit.getAllComponents().size());

        LOG.info(AnsiOutput.toString(AnsiColor.BRIGHT_MAGENTA, "Home Assistant online according to published entities:", AnsiColor.DEFAULT));
        this.states().forEach(state -> {
            LOG.info(AnsiOutput.toString(AnsiColor.BRIGHT_MAGENTA, "\t", state.getEntity_id(), AnsiColor.DEFAULT));
        });

        LOG.info(AnsiOutput.toString(AnsiColor.BRIGHT_GREEN, "Home Assistant startup complete ", AnsiColor.BRIGHT_WHITE, "(url: http://" + this.getHost() + ":", this.getPort(), ")", AnsiColor.DEFAULT));

        Config config = this.config();
        LOG.info(AnsiOutput.toString(AnsiColor.BRIGHT_MAGENTA, "Home Assistant version: ", AnsiColor.BRIGHT_WHITE, config.getVersion(), AnsiColor.DEFAULT));
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
        String uri = "/states/" + id;

        logSimulate(uri);

        return haWebClient.get().uri(uri).retrieve().toEntity(Entity.class).block().getBody();
    }

    private void logSimulate(String uri) {
        LOG.info(AnsiOutput.toString("[", AnsiColor.YELLOW, "GET", AnsiColor.DEFAULT, "] " + apiUrl + uri, " - simulate with:\n\t\t", AnsiColor.BRIGHT_WHITE,
                "curl -X GET -H 'Authorization: Bearer " + BEARER + "' -H 'Content-Type: application/json' " + apiUrl + uri + " | jq .", AnsiColor.DEFAULT));
    }

    public Config config() {
        String uri = "/config";

        logSimulate(uri);

        return haWebClient.get().uri(uri).retrieve().toEntity(Config.class).block().getBody();
    }

    public void post(Function function, int number, String type, Entity entity) {
        String id = getId(function, number, type);

        String uri = "/states/" + id;

        String body;
        try {
            body = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL).writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String result = haWebClient.post().uri("/states/" + id).bodyValue(body).retrieve().toEntity(String.class).block().getBody();

        LOG.info(() -> AnsiOutput.toString(
                AnsiColor.BRIGHT_GREEN, "[POST      ]",
                "\n\t", AnsiColor.BLUE, uri,
                "\n", AnsiColor.BRIGHT_YELLOW, StringUtilities.indent(entity.toPrettyString()),
                "\n\t", AnsiColor.GREEN, "Response:",
                "\n", AnsiColor.BRIGHT_GREEN, StringUtilities.indent(StringUtilities.prettyString(result)),
                "\n", AnsiColor.BRIGHT_CYAN, "\tcurl -X POST \\\n\t\t-H 'Authorization: Bearer " + BEARER + "' \\\n\t\t-H 'Content-Type: application/json' \\\n\t\t-d '" + body + "' " + apiUrl + uri + " | jq .", AnsiColor.DEFAULT));
    }

    public Entity state(Function function, int number, String type) {
        return state(getId(function, number, type));
    }

    @NotNull
    public static String getId(Function function, int number, String type) {
        return type + ".teletask_man_test_localhost_" + function.toString().toLowerCase() + "_" + number;
    }

    public List<Entity> states() {
        return Objects.requireNonNull(haWebClient.get().uri("/states").retrieve().toEntityList(Entity.class).block().getBody()).stream().filter(e -> e.getEntity_id().contains("teletask")).toList();
    }

    public String statesAsString() {
        return haWebClient.get().uri("/states").retrieve().bodyToMono(String.class).block();
    }

    public HomeAssistantExpectationBuilder with() {
        return new HomeAssistantExpectationBuilder(this);
    }

    public HaWebFunctions web() {
        return new HaWebFunctions(configuration, centralUnit);
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

                public EntityObjectSetBuilder set() {
                    return new EntityObjectSetBuilder(this);
                }

                public void match(String describe, Predicate<Entity> matcher) {
                    Entity entity = entityBuilder.homeAssistantBuilder.container.state(entityBuilder.function, entityBuilder.number, type);

                    String message = AnsiOutput.toString("[%s]", AnsiColor.DEFAULT, " Entity '", AnsiColor.BRIGHT_CYAN, entity.getEntity_id(), AnsiColor.DEFAULT, "' expected to have: ", AnsiColor.BRIGHT_YELLOW, describe, AnsiColor.DEFAULT);

                    try {
                        Awaitility.await(describe)
                                .pollInterval(250, TimeUnit.MILLISECONDS)
                                .atMost(10, TimeUnit.SECONDS)
                                .until(() -> matcher.test(entity));
                        LOG.info(AnsiOutput.toString(AnsiColor.BRIGHT_GREEN, String.format(message, "SUCCESS"), AnsiColor.DEFAULT));
                    } catch (Exception e) {
                        LOG.error(AnsiOutput.toString(AnsiColor.BRIGHT_RED, String.format(message, "FAILED"), " - but was: ", AnsiColor.RED, entity, AnsiColor.DEFAULT));
                        throw e;
                    }
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

                public void post(Consumer<Entity> consumer) {
                    Entity entity = new Entity();

                    consumer.accept(entity);

                    entityBuilder.homeAssistantBuilder.container.post(entityBuilder.function, entityBuilder.number, type, entity);
                }

                public static class EntityObjectSetBuilder {

                    private final EntityTypeExpectationBuilder entityTypeBuilder;

                    public EntityObjectSetBuilder(EntityTypeExpectationBuilder entityTypeBuilder) {
                        this.entityTypeBuilder = entityTypeBuilder;
                    }

                    public void on() {
                        this.state(OnOffToggleStateCalculator.ValidOnOffToggle.ON.toString());
                    }

                    public void off() {
                        this.state(OnOffToggleStateCalculator.ValidOnOffToggle.OFF.toString());
                    }

                    public void state(String state) {
                        this.entityTypeBuilder.post(e -> e.setState(state));
                    }
                }
            }
        }
    }

}
