package io.github.ridiekel.jeletask.mqtt;

import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.mqtt.container.TestContainers;
import io.github.ridiekel.jeletask.mqtt.container.ha.Entity;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessor;
import io.github.ridiekel.jeletask.server.TeletaskTestServer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Lazy;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.github.ridiekel.jeletask.server.ExpectationBuilder.ExpectationResponseBuilder.state;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.get;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.groupGet;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.set;

@ActiveProfiles("test")
@SpringBootTest
class TeletaskServiceTest {
    @Autowired
    private TestContainers testContainers;
    @Autowired
    private TeletaskTestServer server;

    @Autowired
    private MqttProcessor mqttProcessor;

    @Autowired
    private TeletaskClient teletaskClient;

    public TeletaskServiceTest() {
        System.out.println("testContainers = " + testContainers);
    }


//    @DynamicPropertySource
//    public static void setupProperties(DynamicPropertyRegistry registry) {
//        registry.add("teletask.mqtt.port", () -> this.testContainers.mqtt().getPort());
//    }

    @Test
    void one() throws InterruptedException, IOException {
        this.server.mock(e -> {
                    e.expect(Function.RELAY, 1, "ON");
                    e.when(get(Function.RELAY, 1)).thenRespond(state(Function.RELAY, 1, "ON"));
                    e.when(groupGet(Function.RELAY, 1, 2)).thenRespond(
                            state(Function.RELAY, 1, "ON"),
                            state(Function.RELAY, 2, "OFF")
                    );
                }
        );


//        System.out.println(TestContainers.ha().statesString());


        System.out.println(this.testContainers.ha().states().stream().map(Entity::entity_id).collect(Collectors.toList()));


//        Runtime.getRuntime().exec("xdg-open http://localhost:"+TestContainers.ha().getPort());
//        Thread.sleep(Long.MAX_VALUE);
    }
}

