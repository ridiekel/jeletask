package io.github.ridiekel.jeletask.client;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator.ValidOnOffToggle;
import io.github.ridiekel.jeletask.client.builder.composer.microsplus.MicrosPlusMessageHandler;
import io.github.ridiekel.jeletask.client.builder.message.strategy.KeepAliveStrategy;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.CentralUnitType;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A central unit with a few components and a keep-alive strategy of the test's choosing, which the production handler
 * fixes at one minute.
 */
public final class TestCentralUnit extends CentralUnit {
    public static final int RELAY_1 = 1;
    public static final int RELAY_2 = 2;
    public static final int RELAY_WITHOUT_STATE = 3;
    public static final int DIMMER_1 = 7;
    public static final int DISPLAY = 5;
    public static final int DISPLAY_WITHOUT_ADDRESS = 6;

    private final MessageHandler handler;

    private TestCentralUnit(int port, KeepAliveStrategy keepAliveStrategy) {
        this.handler = new MicrosPlusMessageHandler() {
            @Override
            public KeepAliveStrategy getKeepAliveStrategy() {
                return keepAliveStrategy;
            }
        };
        this.setType(CentralUnitType.MICROS_PLUS);
        this.setHost("127.0.0.1");
        this.setPort(port);
        this.setVersion("test");

        ComponentSpec display = component(DISPLAY, null);
        display.setBus_numbers("1,2");
        display.setAddress_numbers("3,4");

        Map<Function, List<ComponentSpec>> components = new LinkedHashMap<>();
        components.put(Function.RELAY, List.of(
                component(RELAY_1, new OnOffState(ValidOnOffToggle.OFF)),
                component(RELAY_2, new OnOffState(ValidOnOffToggle.OFF)),
                component(RELAY_WITHOUT_STATE, null)));
        components.put(Function.DIMMER, List.of(component(DIMMER_1, null)));
        components.put(Function.DISPLAYMESSAGE, List.of(display, component(DISPLAY_WITHOUT_ADDRESS, null)));
        this.setComponentsTypes(components);
    }

    public static TestCentralUnit on(int port) {
        return new TestCentralUnit(port, MicrosPlusMessageHandler.KEEP_ALIVE_STRATEGY);
    }

    public static TestCentralUnit withKeepAliveEvery(int port, int intervalMillis) {
        KeepAliveStrategy production = MicrosPlusMessageHandler.KEEP_ALIVE_STRATEGY;
        return new TestCentralUnit(port, new KeepAliveStrategy() {
            @Override
            public int getIntervalMillis() {
                return intervalMillis;
            }

            @Override
            public void execute(TeletaskClientImpl client) {
                production.execute(client);
            }
        });
    }

    /**
     * For tests that silence the central unit or break the connection themselves: a keep-alive would answer that with a reconnect.
     */
    public static TestCentralUnit withoutKeepAlive(int port) {
        return new TestCentralUnit(port, new KeepAliveStrategy() {
            @Override
            public int getIntervalMillis() {
                return 60_000;
            }

            @Override
            public void execute(TeletaskClientImpl client) {
                // Nothing to send
            }
        });
    }

    public static TestCentralUnit withKeepAlive(int port, KeepAliveStrategy keepAliveStrategy) {
        return new TestCentralUnit(port, keepAliveStrategy);
    }

    /**
     * The raw event the central unit sends when the state of a configured component changes.
     */
    public byte[] event(Function function, int number, State<?> state) {
        return this.getMessageHandler().createResponseEventMessageForTesting(this, function, new MessageHandler.OutputState(number, state)).getRawBytes();
    }

    @Override
    public MessageHandler getMessageHandler() {
        return this.handler;
    }

    private static ComponentSpec component(int number, State<?> state) {
        ComponentSpec component = new ComponentSpec();
        component.setNumber(number);
        component.setDescription("Component " + number);
        component.setState(state);
        return component;
    }
}
