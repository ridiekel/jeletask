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
    static {
        System.setProperty("java.awt.headless", "false");
    }

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
        private final TeletaskClient teletaskClient;
        private final MqttContainer mqttContainer;

        public TeletaskTestClient(TeletaskClient teletaskClient, MqttContainer mqttContainer) {
            this.teletaskClient = teletaskClient;
            this.mqttContainer = mqttContainer;
        }

        public FunctionSetBuilder function(Function function, int number) {
            return new FunctionSetBuilder(this, function, number);
        }

        public OnOffSetBuilder relay(int number) {
            return new OnOffSetBuilder(this, Function.RELAY, number);
        }

        public static class FunctionSetBuilder {

            private final Function function;
            private final int number;
            private final TeletaskTestClient testClient;

            public FunctionSetBuilder(TeletaskTestClient testClient, Function function, int number) {
                this.function = function;
                this.number = number;
                this.testClient = testClient;
            }

            protected void set(String state) {
                set(new ComponentState(state));
            }

            protected void set(ComponentState state) {
                testClient.mqttContainer.reset();
                testClient.teletaskClient.set(function, number, state, onSuccess(), onFailSet());
            }
        }

        public static class OnOffSetBuilder extends FunctionSetBuilder {

            public OnOffSetBuilder(TeletaskTestClient testClient, Function function, int number) {
                super(testClient, function, number);
            }

            protected void turnOn() {
                set("ON");
            }

            protected void turnOff() {
                set("OFF");
            }
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

