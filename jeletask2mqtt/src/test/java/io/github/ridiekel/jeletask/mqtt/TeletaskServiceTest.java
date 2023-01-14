package io.github.ridiekel.jeletask.mqtt;

import io.github.ridiekel.jeletask.client.FailureConsumer;
import io.github.ridiekel.jeletask.client.SuccessConsumer;
import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.mqtt.container.ha.HomeAssistantContainer;
import io.github.ridiekel.jeletask.server.TeletaskTestServer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static io.github.ridiekel.jeletask.server.ExpectationBuilder.WithBuilder.get;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.WithBuilder.set;

@ActiveProfiles("test")
@SpringBootTest
class TeletaskServiceTest {
    @Autowired
    private HomeAssistantContainer ha;
    @Autowired
    private TeletaskTestServer server;
    @Autowired
    private TeletaskClient client;

    @Test
    void one() {
        this.server.mock(e -> {
                    e.with(Function.RELAY, 1).when(get()).thenRespond("ON");
                    e.with(Function.RELAY, 1).when(set("OFF")).thenRespond("OFF");
                    e.with(Function.RELAY, 1).when(set("ON")).thenRespond("ON");
                }
        );


        this.client.set(Function.RELAY, 1, new ComponentState("OFF"), onSuccess(), onFail());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

//        System.out.println(ha.statesString());
//        System.out.println(ha.states().stream().map(e -> e.entity_id() + " - "+ e.attributes().friendly_name()).collect(Collectors.toList()));
        System.out.println(ha.state("light.teletask_man_test_localhost_1234_relay_1"));

        this.ha.openBrowser();
//
    }

    @NotNull
    private static FailureConsumer onFail() {
        return (function, number, state, e) -> {
        };
    }

    @NotNull
    private static SuccessConsumer onSuccess() {
        return (function, number, state) -> {
        };
    }
}

