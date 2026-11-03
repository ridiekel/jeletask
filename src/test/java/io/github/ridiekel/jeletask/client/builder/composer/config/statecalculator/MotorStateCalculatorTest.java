package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.MotorStateCalculator.ValidMotorDirectionState;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.MotorStateCalculator.ValidProtectionState;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator.ValidOnOffToggle;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.component.MotorSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.MotorState;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Arrays;

class MotorStateCalculatorTest {
    // direction, power, protection, requested, current, seconds x 100 (2 bytes), correction at 0 %, correction at 100 %
    private static final byte[] FULL_EVENT = {2, (byte) 255, 1, 70, 40, 0x01, 0x2C, 5, 7};

    private final MotorStateCalculator calculator = new MotorStateCalculator();
    private final ComponentSpec component = new MotorSpec();

    private MotorState fromFullEventCutAt(int length) {
        return calculator.fromEvent(component, Arrays.copyOf(FULL_EVENT, length));
    }

    @Test
    void correctionsSurviveAnEvent() {
        // TT15132: direction, power, protection, position and 5 extra bytes, so a correction is 1 byte
        MotorState state = new MotorState(ValidMotorDirectionState.DOWN);
        state.setPower(ValidOnOffToggle.ON);
        state.setProtection(ValidProtectionState.ON_OVERRULED_BY_USER);
        state.setRequestedPosition(70);
        state.setCurrentPosition(40);
        state.setSecondsToFinish(new BigDecimal("3.00"));
        state.setCorrectionAtZeroPercentInSeconds(5);
        state.setCorrectionAtHundredPercentInSeconds(7);

        byte[] event = calculator.toEventForTesting(component, state);

        Assertions.assertThat(event).hasSize(9);
        Assertions.assertThat(calculator.fromEvent(component, event)).isEqualTo(state);
    }

    @Test
    void everyFieldOfAFullEventIsReadFromItsOwnPosition() {
        MotorState state = fromFullEventCutAt(9);

        Assertions.assertThat(state.getState()).isEqualTo(ValidMotorDirectionState.DOWN);
        Assertions.assertThat(state.getPower()).isEqualTo(ValidOnOffToggle.ON);
        Assertions.assertThat(state.getProtection()).isEqualTo(ValidProtectionState.ON_MOTOR_CONTROLLED_BY_PROTECTION);
        Assertions.assertThat(state.getRequestedPosition()).isEqualTo(70);
        Assertions.assertThat(state.getCurrentPosition()).isEqualTo(40);
        Assertions.assertThat(state.getSecondsToFinish()).isEqualByComparingTo("3.00");
        Assertions.assertThat(state.getCorrectionAtZeroPercentInSeconds()).isEqualTo(5);
        Assertions.assertThat(state.getCorrectionAtHundredPercentInSeconds()).isEqualTo(7);
    }

    @Test
    void shorterEventsLeaveTheMissingFieldsEmpty() {
        Assertions.assertThat(fromFullEventCutAt(1).getPower()).isNull();

        Assertions.assertThat(fromFullEventCutAt(2).getPower()).isEqualTo(ValidOnOffToggle.ON);
        Assertions.assertThat(fromFullEventCutAt(2).getProtection()).isNull();

        Assertions.assertThat(fromFullEventCutAt(3).getProtection()).isEqualTo(ValidProtectionState.ON_MOTOR_CONTROLLED_BY_PROTECTION);
        Assertions.assertThat(fromFullEventCutAt(3).getRequestedPosition()).isNull();

        Assertions.assertThat(fromFullEventCutAt(4).getRequestedPosition()).isEqualTo(70);
        Assertions.assertThat(fromFullEventCutAt(4).getCurrentPosition()).isNull();

        Assertions.assertThat(fromFullEventCutAt(5).getCurrentPosition()).isEqualTo(40);
        Assertions.assertThat(fromFullEventCutAt(5).getSecondsToFinish()).isNull();

        // Half of a 2 byte value is not a value
        Assertions.assertThat(fromFullEventCutAt(6).getSecondsToFinish()).isNull();

        Assertions.assertThat(fromFullEventCutAt(7).getSecondsToFinish()).isEqualByComparingTo("3.00");
        Assertions.assertThat(fromFullEventCutAt(7).getCorrectionAtZeroPercentInSeconds()).isNull();

        Assertions.assertThat(fromFullEventCutAt(8).getCorrectionAtZeroPercentInSeconds()).isEqualTo(5);
        Assertions.assertThat(fromFullEventCutAt(8).getCorrectionAtHundredPercentInSeconds()).isNull();
    }

    @Test
    void extraBytesAreIgnored() {
        byte[] event = Arrays.copyOf(FULL_EVENT, 12);
        event[9] = 1;

        Assertions.assertThat(calculator.fromEvent(component, event)).isEqualTo(fromFullEventCutAt(9));
    }

    @Test
    void valuesWithTheHighBitSetAreUnsigned() {
        MotorState state = calculator.fromEvent(component, new byte[]{1, 0, 0, 50, 50, (byte) 0xFF, (byte) 0xFF, (byte) 200, (byte) 255});

        Assertions.assertThat(state.getSecondsToFinish()).isEqualByComparingTo("655.35");
        Assertions.assertThat(state.getCorrectionAtZeroPercentInSeconds()).isEqualTo(200);
        Assertions.assertThat(state.getCorrectionAtHundredPercentInSeconds()).isEqualTo(255);
    }

    @Test
    void requestedPositionAboveHundredIsCappedWhenRead() {
        Assertions.assertThat(calculator.fromEvent(component, new byte[]{1, 0, 0, (byte) 200}).getRequestedPosition()).isEqualTo(100);
    }

    @ParameterizedTest
    @CsvSource({
            "UP,                   1",
            "DOWN,                 2",
            "STOP,                 3",
            "START_STOP,           6",
            "UP_STOP,              7",
            "DOWN_STOP,            8",
            "UP_DOWN,              55",
            "MOTOR_GO_TO_POSITION, 11",
            "MOTOR_SUN_PROTECTION, 15",
    })
    void directionsUseTheValuesOfTheManual(ValidMotorDirectionState direction, int setting) {
        Assertions.assertThat(calculator.toCommand(component, new MotorState(direction))).containsExactly((byte) setting);
        Assertions.assertThat(calculator.fromEvent(component, new byte[]{(byte) setting}).getState()).isEqualTo(direction);
    }

    @ParameterizedTest
    @CsvSource({
            "0, NOT_DEFINED",
            "1, ON_MOTOR_CONTROLLED_BY_PROTECTION",
            "2, ON_MOTOR_NOT_CONTROLLED_BY_PROTECTION",
            "3, ON_OVERRULED_BY_USER",
            "4, SWITCHED_OFF",
    })
    void protectionUsesTheValuesOfMessagesMd(int value, ValidProtectionState expected) {
        Assertions.assertThat(calculator.fromEvent(component, new byte[]{1, 0, (byte) value}).getProtection()).isEqualTo(expected);
    }

    @Test
    void anUnknownDirectionIsRejected() {
        Assertions.assertThatThrownBy(() -> calculator.fromEvent(component, new byte[]{99})).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "1, 1", "25, 25", "99, 99", "100, 100", "101, 100", "150, 100", "-1, 0"})
    void aPositionIsSentAsGoToPositionWithinZeroToHundred(int requested, int expected) {
        MotorState state = calculator.stateFromMessage("{\"state\": \"MOTOR_GO_TO_POSITION\", \"requested_position\": " + requested + "}");

        Assertions.assertThat(calculator.toCommand(component, state)).containsExactly((byte) 11, (byte) expected);
    }

    @Test
    void aPositionWinsFromTheDirection() {
        MotorState state = calculator.stateFromMessage("{\"state\": \"UP\", \"requested_position\": 25}");

        Assertions.assertThat(calculator.toCommand(component, state)).containsExactly((byte) 11, (byte) 25);
    }

    @Test
    void aPositionWithoutADirectionIsEnough() {
        MotorState state = calculator.stateFromMessage("{\"requested_position\": 25}");

        Assertions.assertThat(calculator.toCommand(component, state)).containsExactly((byte) 11, (byte) 25);
    }

    @Test
    void nothingToDoIsRejected() {
        MotorState state = calculator.stateFromMessage("{}");

        Assertions.assertThatThrownBy(() -> calculator.toCommand(component, state)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mockedCentralAppliesADirectionWithoutStartingAPositionRun() {
        MotorState state = calculator.fromCommandForTesting(component, new byte[]{3});

        Assertions.assertThat(state.getState()).isEqualTo(ValidMotorDirectionState.STOP);
        Assertions.assertThat(state.getPower()).isNull();
        Assertions.assertThat(state.getRequestedPosition()).isNull();
    }

    @Test
    void mockedCentralStartsAPositionRun() {
        MotorState state = calculator.fromCommandForTesting(component, new byte[]{11, 25});

        Assertions.assertThat(state.getState()).isEqualTo(ValidMotorDirectionState.MOTOR_GO_TO_POSITION);
        Assertions.assertThat(state.getPower()).isEqualTo(ValidOnOffToggle.ON);
        Assertions.assertThat(state.getRequestedPosition()).isEqualTo(25);
    }

    private static MotorState running() {
        MotorState state = new MotorState(ValidMotorDirectionState.UP);
        state.setPower(ValidOnOffToggle.ON);
        return state;
    }

    @Test
    void anEventWithoutPositionsStopsAfterTheProtection() {
        Assertions.assertThat(calculator.toEventForTesting(component, running())).containsExactly((byte) 1, (byte) 255, (byte) 0);
    }

    @Test
    void aMotorStandingStillReportsItsCurrentPositionAsRequested() {
        MotorState state = running();
        state.setCurrentPosition(30);

        Assertions.assertThat(calculator.toEventForTesting(component, state)).containsExactly((byte) 1, (byte) 255, (byte) 0, (byte) 30, (byte) 30);
    }

    @ParameterizedTest
    @ValueSource(ints = {101, 255})
    void aCurrentPositionAboveHundredIsCappedInAnEvent(int current) {
        MotorState state = running();
        state.setRequestedPosition(50);
        state.setCurrentPosition(current);

        Assertions.assertThat(calculator.toEventForTesting(component, state)).containsExactly((byte) 1, (byte) 255, (byte) 0, (byte) 50, (byte) 100);
    }

    @Test
    void aNegativeCurrentPositionIsCappedInAnEvent() {
        MotorState state = running();
        state.setRequestedPosition(50);
        state.setCurrentPosition(-1);

        Assertions.assertThat(calculator.toEventForTesting(component, state)).containsExactly((byte) 1, (byte) 255, (byte) 0, (byte) 50, (byte) 0);
    }
}
