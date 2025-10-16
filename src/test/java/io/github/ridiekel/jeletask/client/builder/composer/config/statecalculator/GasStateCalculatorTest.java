package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GasStateCalculatorTest {
    private static ComponentSpec componentWith(BigDecimal min, BigDecimal max, String type, Integer decimals) {
        ComponentSpec component = new ComponentSpec();
        component.setGas_min(min);
        component.setGas_max(max);
        component.setGas_type(type);
        component.setDecimals(decimals);
        return component;
    }

    @Test
    void roundTrip1() {
        ComponentSpec component = componentWith(new BigDecimal("10"), new BigDecimal("110"), "4-20ma", 2);

        BigDecimal expected = new BigDecimal("200");
        BigDecimal result = GasStateCalculator.fromEventValue(component, expected);
        BigDecimal actual = GasStateCalculator.toCommandValue(component, result);

        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    void roundTrip2() {
        ComponentSpec component = componentWith(new BigDecimal("10"), new BigDecimal("110"), "0-10v", 2);

        BigDecimal expected = new BigDecimal("200");
        BigDecimal result = GasStateCalculator.fromEventValue(component, expected);
        BigDecimal actual = GasStateCalculator.toCommandValue(component, result);

        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    void roundTrip3() {
        ComponentSpec component = componentWith(new BigDecimal("10"), new BigDecimal("110"), "5-10v", 2);

        BigDecimal expected = new BigDecimal("200");
        BigDecimal result = GasStateCalculator.fromEventValue(component, expected);
        BigDecimal actual = GasStateCalculator.toCommandValue(component, result);

        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    void roundTrip4() {
        ComponentSpec component = componentWith(new BigDecimal("10"), new BigDecimal("110"), "0-20ma", 2);

        BigDecimal expected = new BigDecimal("200");
        BigDecimal result = GasStateCalculator.fromEventValue(component, expected);
        BigDecimal actual = GasStateCalculator.toCommandValue(component, result);

        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    void fromEvent_Value_4To20MA() {
        ComponentSpec component = componentWith(new BigDecimal("10"), new BigDecimal("110"), "4-20ma", 2);
        BigDecimal result = GasStateCalculator.fromEventValue(component, new BigDecimal("200"));

        assertEquals(new BigDecimal("13.41"), result);
    }

    @Test
    void fromEvent_Value_0To10V_defaultDecimals() {
        ComponentSpec component = componentWith(new BigDecimal("0"), new BigDecimal("100"), "0-10v", null);
        BigDecimal result = GasStateCalculator.fromEventValue(component, new BigDecimal("512"));

        assertEquals(new BigDecimal("50.05"), result);
    }

    @Test
    void fromEvent_Value_5To10V_customDecimals() {
        ComponentSpec component = componentWith(new BigDecimal("20"), new BigDecimal("120"), "5-10v", 3);
        BigDecimal result = GasStateCalculator.fromEventValue(component, new BigDecimal("256"));

        assertEquals(new BigDecimal("45.024"), result);
    }

    @Test
    void fromEvent_Value_0To20MA() {
        ComponentSpec component = componentWith(new BigDecimal("5"), new BigDecimal("55"), "0-20ma", 1);
        BigDecimal result = GasStateCalculator.fromEventValue(component, new BigDecimal("880"));

        assertEquals(new BigDecimal("55.0"), result);
    }

    @Test
    void fromEvent_Value_missingMin() {
        ComponentSpec component = componentWith(null, new BigDecimal("100"), "0-10v", 2);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> GasStateCalculator.fromEventValue(component, BigDecimal.ZERO));

        assertEquals("Gas min cannot be null", ex.getMessage());
    }

    @Test
    void fromEvent_Value_missingMax() {
        ComponentSpec component = componentWith(new BigDecimal("0"), null, "0-10v", 2);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> GasStateCalculator.fromEventValue(component, BigDecimal.ZERO));

        assertEquals("Gas max cannot be null", ex.getMessage());
    }

    @Test
    void fromEvent_Value_unknownType() {
        ComponentSpec component = componentWith(new BigDecimal("0"), new BigDecimal("100"), "onbekend", 2);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> GasStateCalculator.fromEventValue(component, BigDecimal.ZERO));

        assertTrue(ex.getMessage().contains("Gas type 'onbekend' not found"));
    }

    @Test
    void toCommandValue_4to20mA() {
        ComponentSpec component = componentWith(new BigDecimal("10"), new BigDecimal("110"), "4-20ma", null);

        BigDecimal result = GasStateCalculator.toCommandValue(component, new BigDecimal("60"));

        assertEquals(new BigDecimal("528"), result);
    }

    @Test
    void toCommandValue_0to10V() {
        ComponentSpec component = componentWith(new BigDecimal("0"), new BigDecimal("100"), "0-10v", null);

        BigDecimal result = GasStateCalculator.toCommandValue(component, new BigDecimal("55"));

        assertEquals(new BigDecimal("563"), result);
    }

    @Test
    void toCommandValue_5to10V() {
        ComponentSpec component = componentWith(new BigDecimal("20"), new BigDecimal("120"), "5-10v", null);

        BigDecimal result = GasStateCalculator.toCommandValue(component, new BigDecimal("70"));

        assertEquals(new BigDecimal("512"), result);
    }

    @Test
    void toCommandValue_0to20mA() {
        ComponentSpec component = componentWith(new BigDecimal("5"), new BigDecimal("45"), "0-20ma", null);

        BigDecimal result = GasStateCalculator.toCommandValue(component, new BigDecimal("25"));

        assertEquals(new BigDecimal("440"), result);
    }

    @Test
    void toCommandValue_missingMin() {
        ComponentSpec component = componentWith(null, new BigDecimal("100"), "0-10v", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> GasStateCalculator.toCommandValue(component, BigDecimal.ZERO));

        assertEquals("Gas min cannot be null", ex.getMessage());
    }

    @Test
    void toCommandValue_missingMax() {
        ComponentSpec component = componentWith(new BigDecimal("0"), null, "0-10v", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> GasStateCalculator.toCommandValue(component, BigDecimal.ZERO));

        assertEquals("Gas max cannot be null", ex.getMessage());
    }

    @Test
    void toCommandValue_unknownType() {
        ComponentSpec component = componentWith(new BigDecimal("0"), new BigDecimal("100"), "onbekend", null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> GasStateCalculator.toCommandValue(component, BigDecimal.ZERO));

        assertTrue(ex.getMessage().contains("Gas type 'onbekend' not found"));
    }
}
