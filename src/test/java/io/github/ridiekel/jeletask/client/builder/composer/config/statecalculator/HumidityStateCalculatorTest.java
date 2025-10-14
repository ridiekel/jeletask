package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.spec.state.impl.HumidityState;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class HumidityStateCalculatorTest {
    private static void roundTrip(String number) {
        HumidityStateCalculator calculator = new HumidityStateCalculator();
        byte[] bytes = calculator.toCommand(new HumidityState(new BigDecimal(number)));
        HumidityState luxState = calculator.fromEvent(null, bytes);

        Assertions.assertThat(luxState.getState()).isEqualTo(new BigDecimal(number));
    }

    @Test
    void byteToHumidity() {
        HumidityStateCalculator calculator = new HumidityStateCalculator();
        HumidityState luxState = calculator.fromEvent(null, new byte[]{0, 56});

        Assertions.assertThat(luxState.getState()).isEqualTo(new BigDecimal("56"));
    }

    @Test
    void luxToByte() {
        HumidityStateCalculator calculator = new HumidityStateCalculator();
        byte[] bytes = calculator.toCommand(new HumidityState(new BigDecimal("65")));

        Assertions.assertThat(bytes[0]).isEqualTo((byte) 0);
        Assertions.assertThat(bytes[1]).isEqualTo((byte) 65);
    }

    @Test
    void roundTrip1() {
        HumidityStateCalculator calculator = new HumidityStateCalculator();
        HumidityState luxState = calculator.fromEvent(null, new byte[]{0, -56});
        byte[] bytes = calculator.toCommand(luxState);

        Assertions.assertThat(bytes[0]).isEqualTo((byte) 0);
        Assertions.assertThat(bytes[1]).isEqualTo((byte) -56);
    }

    @Test
    void roundTrip2() {
        roundTrip("95");
        roundTrip("0");
        roundTrip("100");
    }
}