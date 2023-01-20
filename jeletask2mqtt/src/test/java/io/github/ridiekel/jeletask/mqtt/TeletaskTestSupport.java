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
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
abstract class TeletaskTestSupport {
    @Autowired
    private HomeAssistantContainer ha;
    @Autowired
    private MqttContainer mqtt;
    @Autowired
    private TeletaskTestClient teletask;

    @SuppressWarnings("resource")
    @BeforeEach
    public void reset() {
        this.mqtt().reset();
    }


    @Service
    public static class TeletaskTestClient {
        private final TeletaskClient client;
        private final MqttContainer mqttContainer;

        public TeletaskTestClient(TeletaskClient client, MqttContainer mqttContainer) {
            this.client = client;
            this.mqttContainer = mqttContainer;
        }

        protected void set(Function function, int number, String state) {
            set(function, number, new ComponentState(state));
        }

        protected void set(Function function, int number, ComponentState state) {
            mqttContainer.reset();
            this.client.set(function, number, state, onSuccess(), onFailSet());
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

    protected TeletaskTestClient teletask() {
        return teletask;
    }

    protected HomeAssistantContainer ha() {
        return ha;
    }

    protected MqttContainer mqtt() {
        return mqtt;
    }
}

