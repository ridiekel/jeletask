package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.spec.state.impl.DimmerState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

class DimmerStateCalculatorTest {
    @Test
    void isValidWriteState_WithValidBrightness_ReturnsTrue() {
        withDimmer((c, d) -> List.of(0, 20, 100).forEach(i -> {
            d.setState(DimmerStateCalculator.ValidDimmerState.ON);
            d.setBrightness(i);
            assertThat(c.isValidWriteState(d)).withFailMessage("Brightness of " + i).isTrue();
        }));
    }

    @Test
    void isValidWriteState_WithInvalidBrightness_ReturnsFalse() {
        withDimmer((c, d) -> List.of(-1, 101).forEach(i -> {
            d.setState(DimmerStateCalculator.ValidDimmerState.ON);
            d.setBrightness(i);
            assertThat(c.isValidWriteState(d)).withFailMessage("Brightness of " + i).isFalse();
        }));
    }

    @Test
    void canDeserializeWrongCaseEnum() {
        DimmerStateCalculator dimmerStateCalculator = new DimmerStateCalculator();
        DimmerState dimmerState = dimmerStateCalculator.stateFromMessage("""
                { "state": "off" }
                """);
        assertThat(dimmerState.getState()).isEqualTo(DimmerStateCalculator.ValidDimmerState.OFF);

        dimmerState = dimmerStateCalculator.stateFromMessage("""
                { "state": "On" }
                """);
        assertThat(dimmerState.getState()).isEqualTo(DimmerStateCalculator.ValidDimmerState.ON);

        dimmerState = dimmerStateCalculator.stateFromMessage("""
                { "state": "PREViOUs_STaTE" }
                """);
        assertThat(dimmerState.getState()).isEqualTo(DimmerStateCalculator.ValidDimmerState.PREVIOUS_STATE);
    }

    void withDimmer(BiConsumer<DimmerStateCalculator, DimmerState> tester) {
        DimmerStateCalculator calculator = new DimmerStateCalculator();
        DimmerState state = new DimmerState();

        tester.accept(calculator, state);
    }
}