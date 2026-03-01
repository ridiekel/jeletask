package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.DimmerState;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.DimmerStateCalculator.ValidDimmerState.OFF;
import static io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.DimmerStateCalculator.ValidDimmerState.ON;
import static org.assertj.core.api.Assertions.assertThat;

class DimmerStateCalculatorTest {
    private static ComponentSpec componentWith(Integer number, DimmerStateCalculator.ValidDimmerState state, Integer brightness) {
        ComponentSpec component = new ComponentSpec();
        component.setNumber(number);
        DimmerState dimmerState = new DimmerState(state);
        dimmerState.setBrightness(brightness);
        return component;
    }

    @Test
    void toStateOff() {
        ComponentSpec component = componentWith(1, ON, 54);

        Integer actual = DimmerStateCalculator.determineTargetBrightness(component, new DimmerState(OFF, 60));

        Assertions.assertThat(actual).isEqualTo(0);
    }

    @Test
    void toStateOnWithBrightness() {
        ComponentSpec component = componentWith(1, OFF, 54);

        Integer actual = DimmerStateCalculator.determineTargetBrightness(component, new DimmerState(ON, 60));

        Assertions.assertThat(actual).isEqualTo(60);
    }

    @Test
    void toStateOnWithoutBrightness() {
        ComponentSpec component = componentWith(1, OFF, 54);

        Integer actual = DimmerStateCalculator.determineTargetBrightness(component, new DimmerState(ON));

        Assertions.assertThat(actual).isEqualTo(103);
    }

    @Test
    void fromEvent0() {
        ComponentSpec component = componentWith(1, OFF, 54);

        DimmerState actual = new DimmerStateCalculator().fromEvent(component, NumberConverter.UNSIGNED_BYTE.convert(0));

        Assertions.assertThat(actual.getState()).isEqualTo(OFF);
        Assertions.assertThat(actual.getBrightness()).isEqualTo(0);
    }

    static Stream<Arguments> brightnessValues() {
        return IntStream.range(1, 100).mapToObj(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("brightnessValues")
    @Execution(ExecutionMode.CONCURRENT)
    void fromEventWithBrightness(Integer brightness) {
        ComponentSpec component = componentWith(1, OFF, 0);

        DimmerState actual = new DimmerStateCalculator().fromEvent(component, NumberConverter.UNSIGNED_BYTE.convert(brightness));

        Assertions.assertThat(actual.getState()).isEqualTo(ON);
        Assertions.assertThat(actual.getBrightness()).isEqualTo(brightness);
    }

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