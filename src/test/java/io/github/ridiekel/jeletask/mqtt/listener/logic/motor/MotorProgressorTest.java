package io.github.ridiekel.jeletask.mqtt.listener.logic.motor;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.MotorStateCalculator;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.impl.MotorState;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@SpringBootTest(classes = MotorProgressorTest.class)
@ActiveProfiles("test")
class MotorProgressorTest {
    private static @NotNull List<MotorStateHistory> simulateRun(int fromPosition, int toPosition) {
        MotorState state = new MotorState();
        ComponentSpec motor = new ComponentSpec(Function.MOTOR, state, 1);
        motor.setDescription("Test motor 1");
        state.setPower(OnOffToggleStateCalculator.ValidOnOffToggle.ON);
        state.setState(MotorStateCalculator.ValidMotorDirectionState.MOTOR_GO_TO_POSITION);
        state.setCurrentPosition(fromPosition);
        state.setRequestedPosition(toPosition);
        state.setSecondsToFinish(new BigDecimal(100).divide(Progress.THOUSAND, 3, RoundingMode.HALF_UP));

        List<MotorStateHistory> history = new ArrayList<>();
        MotorProgressor progressor = new MotorProgressor(10, (c, s) -> {
            MotorState currentState = (MotorState) s;
            history.add(new MotorStateHistory(currentState.getPower(), currentState.getCurrentPosition(), currentState.getSecondsToFinish()));
        });

        new Timer("motor-service").schedule(progressor, 0, 10);

        progressor.update(motor);

        Awaitility.await("Motor action finished").atMost(5, TimeUnit.SECONDS).until(() -> !history.isEmpty() && history.getLast().currentPosition.equals(state.getRequestedPosition()));

        Assertions.assertThat(history.getLast()).isEqualTo(new MotorStateHistory(OnOffToggleStateCalculator.ValidOnOffToggle.OFF, toPosition, BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY)));

        return history;
    }

    @Test
    void motorProgressor_Fixed_DOWN() {
        simulateRun(72, 63);
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @Execution(ExecutionMode.CONCURRENT)
    void motorProgressor(int from, int to) {
        simulateRun(from, to);
    }

    static Stream<Arguments> arguments() {
        return combine(List.of(99, 70, 66, 59, 50, 45, 33, 25, 10, 1, 0)).stream();
    }

    public static <T> List<Arguments> combine(List<T> items) {
        List<Arguments> resultaat = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            for (int j = 0; j < items.size(); j++) {
                resultaat.add(Arguments.of(items.get(i), items.get(j)));
            }
        }
        return resultaat;
    }

    @Test
    void motorProgressor_Fixed_UP() {
        List<MotorStateHistory> motorStateHistories = simulateRun(56, 83);
        for (MotorStateHistory motorStateHistory : motorStateHistories) {
            System.out.println(motorStateHistory);
        }
    }

    // Long enough to still be running at the end of a test
    private static final String LONG_RUN = "3600";

    private final List<ComponentSpec> published = new ArrayList<>();
    private final MotorProgressor progressor = new MotorProgressor(10, (c, s) -> published.add(c));

    private static ComponentSpec motor(int number, OnOffToggleStateCalculator.ValidOnOffToggle power, int from, int to, String secondsToFinish) {
        MotorState state = new MotorState(MotorStateCalculator.ValidMotorDirectionState.MOTOR_GO_TO_POSITION);
        state.setPower(power);
        state.setCurrentPosition(from);
        state.setRequestedPosition(to);
        state.setSecondsToFinish(secondsToFinish == null ? null : new BigDecimal(secondsToFinish));
        ComponentSpec motor = new ComponentSpec(Function.MOTOR, state, number);
        motor.setDescription("Test motor " + number);
        return motor;
    }

    @Test
    void nothingIsPublishedWithoutARunningMotor() {
        progressor.run();

        Assertions.assertThat(published).isEmpty();
    }

    @Test
    void aMotorThatIsNotOnIsNotTracked() {
        progressor.update(motor(1, OnOffToggleStateCalculator.ValidOnOffToggle.OFF, 10, 20, LONG_RUN));
        progressor.update(motor(2, null, 10, 20, LONG_RUN));

        progressor.run();

        Assertions.assertThat(published).isEmpty();
    }

    @Test
    void aRunningMotorIsPublishedOnEveryRun() {
        ComponentSpec motor = motor(1, OnOffToggleStateCalculator.ValidOnOffToggle.ON, 10, 20, LONG_RUN);
        progressor.update(motor);

        progressor.run();
        progressor.run();

        Assertions.assertThat(published).containsExactly(motor, motor);
    }

    @Test
    void aMotorThatFinishedIsPublishedOnceMoreAsOffAndThenForgotten() {
        ComponentSpec motor = motor(1, OnOffToggleStateCalculator.ValidOnOffToggle.ON, 20, 20, LONG_RUN);
        progressor.update(motor);

        progressor.run();
        Assertions.assertThat(((MotorState) motor.getState()).getPower()).isEqualTo(OnOffToggleStateCalculator.ValidOnOffToggle.OFF);
        progressor.run();

        Assertions.assertThat(published).containsExactly(motor);
    }

    @Test
    void anEventThatSaysTheMotorStoppedEndsTheRun() {
        ComponentSpec motor = motor(1, OnOffToggleStateCalculator.ValidOnOffToggle.ON, 10, 20, LONG_RUN);
        progressor.update(motor);

        ((MotorState) motor.getState()).setPower(OnOffToggleStateCalculator.ValidOnOffToggle.OFF);
        progressor.update(motor);
        progressor.run();

        Assertions.assertThat(published).isEmpty();
    }

    @Test
    void aNewEventReplacesTheRunOfTheSameMotor() {
        ComponentSpec motor = motor(1, OnOffToggleStateCalculator.ValidOnOffToggle.ON, 10, 20, LONG_RUN);
        progressor.update(motor);
        progressor.update(motor);

        progressor.run();

        Assertions.assertThat(published).containsExactly(motor);
    }

    @Test
    void motorsAreTrackedIndependently() {
        ComponentSpec finished = motor(1, OnOffToggleStateCalculator.ValidOnOffToggle.ON, 20, 20, LONG_RUN);
        ComponentSpec running = motor(2, OnOffToggleStateCalculator.ValidOnOffToggle.ON, 10, 20, LONG_RUN);
        progressor.update(finished);
        progressor.update(running);

        progressor.run();
        Assertions.assertThat(published).containsExactlyInAnyOrder(finished, running);

        published.clear();
        progressor.run();
        Assertions.assertThat(published).containsExactly(running);
    }

    @Test
    void aRunThatCannotBeCalculatedIsIgnored() {
        // A short event from the central: running, but without the bytes for the time left
        ComponentSpec noTime = motor(1, OnOffToggleStateCalculator.ValidOnOffToggle.ON, 10, 20, null);
        ComponentSpec noPosition = motor(2, OnOffToggleStateCalculator.ValidOnOffToggle.ON, 10, 20, LONG_RUN);
        ((MotorState) noPosition.getState()).setCurrentPosition(null);

        Assertions.assertThatCode(() -> {
            progressor.update(noTime);
            progressor.update(noPosition);
        }).doesNotThrowAnyException();
        progressor.run();

        Assertions.assertThat(published).isEmpty();
    }

    private record MotorStateHistory(
            OnOffToggleStateCalculator.ValidOnOffToggle power,
            Integer currentPosition,
            BigDecimal secondsToFinish
    ) {

    }


}
