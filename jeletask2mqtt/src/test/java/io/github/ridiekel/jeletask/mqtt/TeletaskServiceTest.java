package io.github.ridiekel.jeletask.mqtt;

import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.mqtt.container.TestContainers;
import io.github.ridiekel.jeletask.mqtt.container.ha.HomeAssistantContainer;
import io.github.ridiekel.jeletask.mqtt.listener.MqttProcessor;
import io.github.ridiekel.jeletask.server.TeletaskTestServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.stream.Collectors;

import static io.github.ridiekel.jeletask.server.ExpectationBuilder.WhenBuilder.state;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.WithBuilder.get;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.WithBuilder.set;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.groupGet;

@ActiveProfiles("test")
@SpringBootTest
class TeletaskServiceTest {
    @Autowired
    private HomeAssistantContainer ha;
    @Autowired
    private TeletaskTestServer server;

    @Test
    void one() {
        this.server.mock(e -> {
                    e.with(Function.RELAY, 1).when(set("ON")).thenRespond("ON");
                    e.with(Function.RELAY, 1).when(get()).thenRespond("ON");
                    e.when(groupGet(Function.RELAY, 1, 2)).thenRespond(
                            state(Function.RELAY, 1, "ON"),
                            state(Function.RELAY, 2, "OFF")
                    );
                }
        );


//        System.out.println(ha.statesString());
        System.out.println(ha.states().stream().map(e -> e.attributes().friendly_name()).collect(Collectors.toList()));


    }
}

