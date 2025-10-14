package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.TemperatureState;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

class TemperatureStateCalculatorTest {
    @Test
    void bytesToTemp() {
        TemperatureStateCalculator c = new TemperatureStateCalculator();
        ComponentSpec component = new ComponentSpec();
        component.setDecimals(5);
        TemperatureState state = c.fromEvent(component, new byte[]{23, 6});

        Assertions.assertThat(state.getState()).isEqualTo(new BigDecimal("316.4").setScale(component.getDecimals(), RoundingMode.UNNECESSARY));
    }

    @Test
    void bytesToTempNoDecimals() {
        TemperatureStateCalculator c = new TemperatureStateCalculator();
        ComponentSpec component = new ComponentSpec();
        component.setDecimals(0);
        TemperatureState state = c.fromEvent(component, new byte[]{23, 6});

        Assertions.assertThat(state.getState()).isEqualTo(new BigDecimal("316"));
    }

    @Test
    void tempToBytes() {
        TemperatureStateCalculator c = new TemperatureStateCalculator();
        ComponentSpec component = new ComponentSpec();
        component.setDecimals(5);
        byte[] bytes = c.toCommand(new TemperatureState(new BigDecimal("30.89")));

        Assertions.assertThat(bytes[0]).isEqualTo((byte) 11);
        Assertions.assertThat(bytes[1]).isEqualTo((byte) -34);
    }

    @Test
    void tempToBytesNoDecimals() {
        TemperatureStateCalculator c = new TemperatureStateCalculator();
        ComponentSpec component = new ComponentSpec();
        component.setDecimals(0);
        byte[] bytes = c.toCommand(new TemperatureState(new BigDecimal("30.89")));

        Assertions.assertThat(bytes[0]).isEqualTo((byte) 11);
        Assertions.assertThat(bytes[1]).isEqualTo((byte) -34);
    }

    @Test
    void roundTrip1() {
        TemperatureStateCalculator c = new TemperatureStateCalculator();
        ComponentSpec component = new ComponentSpec();
        component.setDecimals(5);

        byte[] bytes = c.toCommand(new TemperatureState(new BigDecimal("25.6")));
        TemperatureState state = c.fromEvent(component, bytes);

        Assertions.assertThat(state.getState()).isEqualTo(new BigDecimal("25.6").setScale(component.getDecimals(), RoundingMode.UNNECESSARY));
    }

    @Test
    void roundTrip2() {
        TemperatureStateCalculator c = new TemperatureStateCalculator();
        ComponentSpec component = new ComponentSpec();
        component.setDecimals(5);

        TemperatureState state = c.fromEvent(component, new byte[]{0, 20});
        byte[] bytes = c.toCommand(state);

        Assertions.assertThat(bytes[0]).isEqualTo((byte) 0);
        Assertions.assertThat(bytes[1]).isEqualTo((byte) 20);
    }

}