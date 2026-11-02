package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.spec.state.impl.HumidityState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MESSAGES.md: the humidity sensor (TDS12260) reports a value in %, read as an unsigned short.
 */
class HumidityStateCalculatorRangeTest {
    private final HumidityStateCalculator calculator = new HumidityStateCalculator();

    private static HumidityState humidity(String value) {
        return new HumidityState(new BigDecimal(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "56", "99", "100"})
    void percentagesCanBeWritten(String value) {
        assertThat(this.calculator.isValidWriteState(humidity(value))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "101", "255", "65535"})
    void valuesOutsideZeroToHundredCannotBeWritten(String value) {
        assertThat(this.calculator.isValidWriteState(humidity(value))).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"00 FF, 255", "01 00, 256", "FF FF, 65535"})
    void bothBytesAreReadUnsigned(String hex, long expected) {
        // Characterization: the "-1 means 255" guard in fromEvent never fires, an unsigned short is never -1
        assertThat(this.calculator.fromEvent(null, Bytes.hexToBytes(hex)).getState()).isEqualTo(BigDecimal.valueOf(expected));
    }
}
