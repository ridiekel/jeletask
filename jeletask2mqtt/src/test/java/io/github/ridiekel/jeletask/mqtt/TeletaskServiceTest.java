package io.github.ridiekel.jeletask.mqtt;

import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.mqtt.container.ha.Entity;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("resource")
class TeletaskServiceTest extends TeletaskTestSupport {
    @Test
    void relayStateChange() {
        setViaTeletask(Function.RELAY, 1, new ComponentState("OFF"));

        this.mqtt().expectLastStateMessage(Function.RELAY, 1).toHaveState("OFF");

        setViaTeletask(Function.RELAY, 1, new ComponentState("ON"));

        this.mqtt().expectLastStateMessage(Function.RELAY, 1).toHaveState("ON");

        this.ha().expectEntity(Function.RELAY, 1, "light").toHaveState("ON");

//        Awaitility.await().atMost(10, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
//            Entity entity = ha().state(Function.RELAY, 1, "light");
//            return !Objects.equals(entity.getState(), "unknown");
//        });

//        this.ha().openBrowser();
    }
}

