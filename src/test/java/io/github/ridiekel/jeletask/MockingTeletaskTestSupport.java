package io.github.ridiekel.jeletask;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import io.github.ridiekel.jeletask.client.FailureConsumer;
import io.github.ridiekel.jeletask.client.SuccessConsumer;
import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.InputStateCalculator;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.MotorStateCalculator;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.*;
import io.github.ridiekel.jeletask.mockserver.TeletaskMockServer;
import io.github.ridiekel.jeletask.mqtt.container.mqtt.MqttContainer;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static com.codeborne.selenide.Selenide.$$;

@SpringBootTest(classes = {Teletask2MqttTestApplication.class})
@ActiveProfiles("test")
public class MockingTeletaskTestSupport extends TeletaskTestSupport {
    static {
        System.setProperty("teletask.test.startbrowser", "false");
        System.setProperty("spring.boot.admin.client.enabled", "false");
    }

    @Autowired
    private TeletaskTestClient teletask;

    @Autowired
    protected Teletask2MqttConfigurationProperties config;

    protected TeletaskTestClient teletask() {
        return teletask;
    }

    @Service
    public static class TeletaskTestClient {
        private final TeletaskClient teletaskClient;

        public TeletaskTestClient(TeletaskClient teletaskClient) {
            this.teletaskClient = teletaskClient;
        }

        public FunctionSetBuilder function(Function function, int number) {
            return new FunctionSetBuilder(this, function, number);
        }

        public OnOffSetBuilder relay(int number) {
            return new OnOffSetBuilder(this, Function.RELAY, number);
        }

        public OnOffSetBuilder localmood(int number) {
            return new OnOffSetBuilder(this, Function.LOCMOOD, number);
        }

        public OnOffSetBuilder generalmood(int number) {
            return new OnOffSetBuilder(this, Function.GENMOOD, number);
        }

        public OnOffSetBuilder condition(int number) {
            return new OnOffSetBuilder(this, Function.COND, number);
        }

        public InputSetBuilder input(int number) {
            return new InputSetBuilder(this, Function.INPUT, number);
        }

        public LightSensorSetBuilder lightSensor(int number) {
            return new LightSensorSetBuilder(this, Function.SENSOR, number);
        }

        public TemperatureSensorSetBuilder temperatureSensor(int number) {
            return new TemperatureSensorSetBuilder(this, Function.SENSOR, number);
        }

        public HumiditySensorSetBuilder humiditySensor(int number) {
            return new HumiditySensorSetBuilder(this, Function.SENSOR, number);
        }

        public GasSensorSetBuilder gasSensor(int number) {
            return new GasSensorSetBuilder(this, Function.SENSOR, number);
        }

        public OnOffSetBuilder flag(int number) {
            return new OnOffSetBuilder(this, Function.FLAG, number);
        }

        public DimmerSetBuilder dimmer(int number) {
            return new DimmerSetBuilder(this, number);
        }

        public MotorSetBuilder motor(int number) {
            return new MotorSetBuilder(this, number);
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

            protected void set(Enum<?> state) {
                set(this.testClient.teletaskClient.getCentralUnit().stateFromMessage(function, number, TeletaskMockServer.template("state").apply(state.toString())));
            }

            protected void set(State<?> state) {
                testClient.teletaskClient.set(function, number, state, onSuccess(), onFailSet());
            }
        }

        public static class DimmerSetBuilder extends OnOffSetBuilder {
            public DimmerSetBuilder(TeletaskTestClient testClient, int number) {
                super(testClient, Function.DIMMER, number);
            }

            public void brightness(int brightness) {
                set(new DimmerState(brightness));
            }
        }

        public static class MotorSetBuilder extends OnOffSetBuilder {
            public MotorSetBuilder(TeletaskTestClient testClient, int number) {
                super(testClient, Function.MOTOR, number);
            }

            public void up() {
                MotorState state = new MotorState(MotorStateCalculator.ValidMotorDirectionState.UP);
                state.setRequestedPosition(0);
                set(state);
            }

            public void down() {
                MotorState state = new MotorState(MotorStateCalculator.ValidMotorDirectionState.DOWN);
                state.setRequestedPosition(100);
                set(state);
            }
        }

        public static class OnOffSetBuilder extends FunctionSetBuilder {
            private final Enum<?> on;
            private final Enum<?> off;

            public OnOffSetBuilder(TeletaskTestClient testClient, Function function, int number) {
                this(testClient, function, number, OnOffToggleStateCalculator.ValidOnOffToggle.ON, OnOffToggleStateCalculator.ValidOnOffToggle.OFF);
            }

            public OnOffSetBuilder(TeletaskTestClient testClient, Function function, int number, Enum<?> on, Enum<?> off) {
                super(testClient, function, number);
                this.on = on;
                this.off = off;
            }

            public void turnOn() {
                set(on);
            }

            public void turnOff() {
                set(off);
            }
        }

        public static class InputSetBuilder extends FunctionSetBuilder {

            public InputSetBuilder(TeletaskTestClient testClient, Function function, int number) {
                super(testClient, function, number);
            }

            public void open() {
                set(InputStateCalculator.ValidInputState.OPEN);
            }

            public void close() {
                set(InputStateCalculator.ValidInputState.CLOSED);
            }
        }

        public static class LightSensorSetBuilder extends FunctionSetBuilder {

            public LightSensorSetBuilder(TeletaskTestClient testClient, Function function, int number) {
                super(testClient, function, number);
            }

            public void update(BigDecimal value) {
                set(new LuxState(value));
            }
        }

        public static class TemperatureSensorSetBuilder extends FunctionSetBuilder {

            public TemperatureSensorSetBuilder(TeletaskTestClient testClient, Function function, int number) {
                super(testClient, function, number);
            }

            public void update(BigDecimal value) {
                set(new TemperatureState(value));
            }
        }

        public static class HumiditySensorSetBuilder extends FunctionSetBuilder {

            public HumiditySensorSetBuilder(TeletaskTestClient testClient, Function function, int number) {
                super(testClient, function, number);
            }

            public void update(BigDecimal value) {
                set(new HumidityState(value));
            }
        }

        public static class GasSensorSetBuilder extends FunctionSetBuilder {

            public GasSensorSetBuilder(TeletaskTestClient testClient, Function function, int number) {
                super(testClient, function, number);
            }

            public void update(BigDecimal value) {
                set(new GasState(value));
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

    @BeforeEach
    void login() {
        ha().login();
        $$(Selectors.shadowDeepCss("hui-entities-card div.name")).shouldHave(CollectionCondition.size(6));
        Assertions.assertThat($$(Selectors.shadowDeepCss("hui-entities-card div.name")).texts()).containsExactlyInAnyOrder(
                "Binary sensor",
                "Cover",
                "Light",
                "Switch",
                "Scene",
                "Sensor"
        );
    }
}
