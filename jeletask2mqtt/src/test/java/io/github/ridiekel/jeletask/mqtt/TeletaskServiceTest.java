package io.github.ridiekel.jeletask.mqtt;

import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import org.junit.jupiter.api.Test;

class TeletaskServiceTest extends TeletaskTestSupport {
    @Test
    void relayStateChange() {
        System.out.println(ha().state("light.teletask_man_test_localhost_1234_relay_1"));

        set(Function.RELAY, 1, new ComponentState("OFF"));

        System.out.println(ha().state("light.teletask_man_test_localhost_1234_relay_1"));

        this.ha().openBrowser();
    }
}

