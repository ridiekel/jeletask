package io.github.ridiekel.jeletask.mqtt;

import io.github.ridiekel.jeletask.client.FailureConsumer;
import io.github.ridiekel.jeletask.client.SuccessConsumer;
import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.mqtt.container.ha.HomeAssistantContainer;
import io.github.ridiekel.jeletask.mqtt.container.mqtt.MqttContainer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
abstract class TeletaskTestSupport {
    @Autowired
    private HomeAssistantContainer ha;
    @Autowired
    private MqttContainer mqtt;
    @Autowired
    private TeletaskClient client;

    @SuppressWarnings("resource")
    @BeforeEach
    public void reset() {
        this.mqtt().reset();
    }

    protected void setViaTeletask(Function function, int number, String state) {
        setViaTeletask(function, number, new ComponentState(state));
    }

    protected void setViaTeletask(Function function, int number, ComponentState state) {
        this.client.set(function, number, state, onSuccess(), onFailSet());
    }

    protected HomeAssistantContainer ha() {
        return ha;
    }

    protected MqttContainer mqtt() {
        return mqtt;
    }

    @NotNull
    private static FailureConsumer onFailSet() {
        return (function, number, state, e) -> {
            throw new RuntimeException(String.format("Failure to set %s(%s) to: %s", function, number, state), e);
        };
    }

    @NotNull
    private static SuccessConsumer onSuccess() {
        return (function, number, state) -> {
        };
    }
}

