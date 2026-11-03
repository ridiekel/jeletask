package io.github.ridiekel.jeletask.mqtt.listener.command;

import io.github.ridiekel.jeletask.client.FailureConsumer;
import io.github.ridiekel.jeletask.client.SuccessConsumer;
import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.listener.StateChangeListener;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.CentralUnitType;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.component.DisplayMessageSpec;
import io.github.ridiekel.jeletask.client.spec.component.RelaySpec;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.DisplayMessageState;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;
import org.assertj.core.api.Assertions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class MQTTMessageToTeletaskCommandTest {
    private static final String BASE = "prefix/central";
    private static final String ON = "{\"state\": \"ON\"}";

    private final FakeTeletaskClient client = new FakeTeletaskClient(centralUnit());
    private final MQTTMessageToTeletaskCommand command = new MQTTMessageToTeletaskCommand(client, "prefix", "central");

    private static CentralUnit centralUnit() {
        ComponentSpec relay = new RelaySpec();
        relay.setNumber(1);
        relay.setDescription("Relay 1");
        ComponentSpec display = new DisplayMessageSpec();
        display.setNumber(2);
        display.setDescription("Display 2");

        Map<Function, List<ComponentSpec>> components = new LinkedHashMap<>();
        components.put(Function.RELAY, List.of(relay));
        components.put(Function.DISPLAYMESSAGE, List.of(display));

        CentralUnit centralUnit = new CentralUnit();
        centralUnit.setType(CentralUnitType.MICROS_PLUS);
        centralUnit.setVersion("test");
        centralUnit.setComponentsTypes(components);
        return centralUnit;
    }

    private void arrive(String topic, String payload) {
        command.messageArrived(topic, new MqttMessage(payload.getBytes()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"relay", "RELAY", "Relay"})
    void aStateIsSetOnTheComponentOfTheTopic(String function) {
        arrive(BASE + "/" + function + "/1/set", ON);

        Assertions.assertThat(client.calls).containsExactly(new Call("set", Function.RELAY, 1, new OnOffState(OnOffToggleStateCalculator.ValidOnOffToggle.ON)));
    }

    @Test
    void aDisplayMessageIsNotSetButDisplayed() {
        arrive(BASE + "/displaymessage/2/set", "{\"message_line1\": \"Hello\"}");

        DisplayMessageState expected = new DisplayMessageState();
        expected.setMessageLine1("Hello");
        Assertions.assertThat(client.calls).containsExactly(new Call("displaymessage", Function.DISPLAYMESSAGE, 2, expected));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Another central or prefix
            "prefix/other/relay/1/set",
            "other/central/relay/1/set",
            // Not a set topic
            BASE + "/relay/1/state",
            BASE + "/relay/1",
            // Not a function or number
            BASE + "/relay/abc/set",
            BASE + "/relay/-1/set",
            BASE + "/relay//set",
            BASE + "//1/set",
            BASE + "/nosuchfunction/1/set",
            // Number does not fit in an int
            BASE + "/relay/99999999999/set",
            // Not in the configuration
            BASE + "/relay/99/set",
            BASE + "/motor/1/set",
    })
    void anUnusableTopicDoesNothing(String topic) {
        arrive(topic, ON);

        Assertions.assertThat(client.calls).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "ON",
            "{not json",
            "{\"state\": \"NOSUCHSTATE\"}",
    })
    void anUnusablePayloadDoesNothing(String payload) {
        arrive(BASE + "/relay/1/set", payload);

        Assertions.assertThat(client.calls).isEmpty();
    }

    @Test
    void aFailingClientDoesNotReachTheMqttThread() {
        // An exception out of messageArrived would make Paho drop the MQTT connection
        client.failure = new IllegalStateException("Central unit is gone");

        Assertions.assertThatCode(() -> arrive(BASE + "/relay/1/set", ON)).doesNotThrowAnyException();
        Assertions.assertThat(client.calls).hasSize(1);
    }

    private record Call(String method, Function function, int number, State<?> state) {
    }

    private static final class FakeTeletaskClient implements TeletaskClient {
        private final CentralUnit centralUnit;
        private final List<Call> calls = new ArrayList<>();
        private RuntimeException failure;

        private FakeTeletaskClient(CentralUnit centralUnit) {
            this.centralUnit = centralUnit;
        }

        private void recordCall(Call call) {
            calls.add(call);
            if (failure != null) {
                throw failure;
            }
        }

        @Override
        public void set(Function function, int number, State<?> state, SuccessConsumer onSuccess, FailureConsumer onFailed) {
            recordCall(new Call("set", function, number, state));
        }

        @Override
        public void displaymessage(int number, DisplayMessageState state, SuccessConsumer onSuccess, FailureConsumer onFailed) {
            recordCall(new Call("displaymessage", Function.DISPLAYMESSAGE, number, state));
        }

        @Override
        public CentralUnit getCentralUnit() {
            return centralUnit;
        }

        @Override
        public void registerStateChangeListener(StateChangeListener listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(ComponentSpec component, State<?> state, SuccessConsumer onSuccess, FailureConsumer onFailed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void displaymessage(ComponentSpec component, DisplayMessageState state, SuccessConsumer onSuccess, FailureConsumer onFailed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void get(Function function, int number, SuccessConsumer onSuccess, FailureConsumer onFailed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void get(ComponentSpec component, SuccessConsumer onSuccess, FailureConsumer onFailed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void groupGet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isConnected() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void disconnect() {
            throw new UnsupportedOperationException();
        }
    }
}
