package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.spec.state.impl.LuxState;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class LuxStateCalculatorTest {
    private static void roundTrip(String number) {
        LuxStateCalculator calculator = new LuxStateCalculator();
        byte[] bytes = calculator.toCommand(new LuxState(new BigDecimal(number)));
        LuxState luxState = calculator.fromEvent(null, bytes);

        Assertions.assertThat(luxState.getState()).isEqualTo(new BigDecimal(number));
    }

    @Test
    void byteToLux() {
        LuxStateCalculator calculator = new LuxStateCalculator();
        LuxState luxState = calculator.fromEvent(null, new byte[]{0, -114});

        Assertions.assertThat(luxState.getState()).isEqualTo(new BigDecimal("3548"));
    }

    @Test
    void luxToByte() {
        LuxStateCalculator calculator = new LuxStateCalculator();
        byte[] bytes = calculator.toCommand(new LuxState(new BigDecimal("2506")));

        Assertions.assertThat(bytes[0]).isEqualTo((byte) 0);
        Assertions.assertThat(bytes[1]).isEqualTo((byte) -121);
    }

    @Test
    void roundTrip1() {
        LuxStateCalculator calculator = new LuxStateCalculator();
        LuxState luxState = calculator.fromEvent(null, new byte[]{0, -56});
        byte[] bytes = calculator.toCommand(luxState);

        Assertions.assertThat(bytes[0]).isEqualTo((byte) 0);
        Assertions.assertThat(bytes[1]).isEqualTo((byte) -56);
    }

    @Test
    void roundTrip2() {
        roundTrip("3548");
        roundTrip("794");
    }
}