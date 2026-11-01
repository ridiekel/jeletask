package io.github.ridiekel.jeletask.mqtt.listener;

import io.github.ridiekel.jeletask.Teletask2MqttConfigurationProperties;
import io.github.ridiekel.jeletask.client.FailureConsumer;
import io.github.ridiekel.jeletask.client.SuccessConsumer;
import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.listener.StateChangeListener;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.CentralUnitType;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.DisplayMessageState;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

final class MqttProcessorFixtures {
    static final String PREFIX = "fixture_prefix";
    static final String CENTRAL_ID = "fixture_central";
    static final String BASE_TOPIC = PREFIX + "/" + CENTRAL_ID;

    private MqttProcessorFixtures() {
    }

    static Teletask2MqttConfigurationProperties configuration() {
        Teletask2MqttConfigurationProperties.MqttConfiguration mqtt = new Teletask2MqttConfigurationProperties.MqttConfiguration();
        mqtt.setPrefix(PREFIX);
        mqtt.setClientId("fixture_client");

        Teletask2MqttConfigurationProperties.CentralConfiguration central = new Teletask2MqttConfigurationProperties.CentralConfiguration();
        central.setId(CENTRAL_ID);

        Teletask2MqttConfigurationProperties configuration = new Teletask2MqttConfigurationProperties();
        configuration.setMqtt(mqtt);
        configuration.setCentral(central);
        configuration.getPublish().setStatesInterval(0);
        return configuration;
    }

    static ComponentSpec relay(int number) {
        ComponentSpec relay = new ComponentSpec(Function.RELAY, new OnOffState(OnOffToggleStateCalculator.ValidOnOffToggle.ON), number);
        relay.setDescription("Relay " + number);
        return relay;
    }

    static CentralUnit centralUnit() {
        CentralUnit centralUnit = new CentralUnit();
        centralUnit.setType(CentralUnitType.MICROS_PLUS);
        centralUnit.setVersion("test");
        Map<Function, List<ComponentSpec>> components = new LinkedHashMap<>();
        components.put(Function.RELAY, List.of(relay(1)));
        centralUnit.setComponentsTypes(components);
        return centralUnit;
    }

    static final class FakeTeletaskClient implements TeletaskClient {
        final AtomicInteger groupGets = new AtomicInteger();
        volatile boolean connected = true;
        volatile boolean groupGetFails;

        @Override
        public void groupGet() {
            groupGets.incrementAndGet();
            if (groupGetFails) {
                throw new IllegalStateException("central unit is away");
            }
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public void registerStateChangeListener(StateChangeListener listener) {
        }

        @Override
        public void disconnect() {
        }

        @Override
        public CentralUnit getCentralUnit() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(ComponentSpec component, State<?> state, SuccessConsumer onSuccess, FailureConsumer onFailed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(Function function, int number, State<?> state, SuccessConsumer onSucccess, FailureConsumer onFailed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void displaymessage(ComponentSpec component, DisplayMessageState state, SuccessConsumer onSuccess, FailureConsumer onFailed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void displaymessage(int number, DisplayMessageState state, SuccessConsumer onSucccess, FailureConsumer onFailed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void get(Function function, int number, SuccessConsumer onSucccess, FailureConsumer onFailed) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void get(ComponentSpec component, SuccessConsumer onSuccess, FailureConsumer onFailed) {
            throw new UnsupportedOperationException();
        }
    }
}
