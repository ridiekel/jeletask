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
        teletask().set(Function.RELAY, 1, "OFF");

        mqtt().expect().lastStateMessage(Function.RELAY, 1).toHave().state("OFF");

        teletask().set(Function.RELAY, 1, "ON");

        mqtt().expect().lastStateMessage(Function.RELAY, 1).toHave().state("ON");

        ha().expect().entity(Function.RELAY, 1, "light").toHave().state("ON");

//        this.ha().openBrowser();
    }
}
