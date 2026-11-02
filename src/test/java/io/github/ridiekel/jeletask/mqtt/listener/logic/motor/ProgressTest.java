package io.github.ridiekel.jeletask.mqtt.listener.logic.motor;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.MotorStateCalculator;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.spec.state.impl.MotorState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.*;

class ProgressTest {
    private static final long NOW = 1_000_000L;
    private static final LongSupplier CLOCK = () -> NOW;

    @Test
    void update_doesNothingWhenDirectionStop() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(25);
        motorState.setRequestedPosition(25);
        motorState.setSecondsToFinish(new BigDecimal("5.0"));

        Progress progress = new Progress(500, motorState, CLOCK);

        progress.update();

        assertSame(Progress.Direction.STOP, progress.getDirection());
        assertEquals(25, motorState.getCurrentPosition());
        assertEquals(0, new BigDecimal("0.0").compareTo(motorState.getSecondsToFinish()));
        assertEquals(0, progress.getLastPosition());
    }

    @Test
    void update_movesDownwardsWithinBounds() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(10);
        motorState.setRequestedPosition(20);
        motorState.setSecondsToFinish(new BigDecimal("10.0"));

        Progress progress = new Progress(500, motorState, CLOCK);
        progress.setStartTimeMillis(NOW - 3000L);

        progress.update();

        assertEquals(13, motorState.getCurrentPosition());
        assertEquals(13, progress.getLastPosition());
        assertEquals(0, new BigDecimal("7.00").compareTo(motorState.getSecondsToFinish()));
        assertSame(Progress.Direction.DOWN, progress.getDirection());
    }

    @Test
    void update_movesUpwardsWithinBounds() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(90);
        motorState.setRequestedPosition(80);
        motorState.setSecondsToFinish(new BigDecimal("5.0"));

        Progress progress = new Progress(200, motorState, CLOCK);
        progress.setStartTimeMillis(NOW - 2000L);

        progress.update();

        assertEquals(86, motorState.getCurrentPosition());
        assertEquals(86, progress.getLastPosition());
        assertEquals(0, new BigDecimal("3.00").compareTo(motorState.getSecondsToFinish()));
        assertSame(Progress.Direction.UP, progress.getDirection());
    }

    @Test
    void update_clampsSecondsToFinishAtZeroWhenElapsedBeyondDuration() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(40);
        motorState.setRequestedPosition(50);
        motorState.setSecondsToFinish(new BigDecimal("6.0"));

        Progress progress = new Progress(250, motorState, CLOCK);
        progress.setStartTimeMillis(NOW - 9000L);

        progress.update();

        assertEquals(BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY), motorState.getSecondsToFinish());
    }

    @Test
    void update_setsSecondsToFinishZeroWhenLessThanInterval() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(10);
        motorState.setRequestedPosition(11);
        motorState.setSecondsToFinish(new BigDecimal("1.0"));

        Progress progress = new Progress(200, motorState, CLOCK);
        progress.setStartTimeMillis(NOW - 850L);

        progress.update();

        assertEquals(11, motorState.getCurrentPosition());
        assertEquals(BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY), motorState.getSecondsToFinish());
    }

    @Test
    void update_doesNotExceedUpperBound() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(99);
        motorState.setRequestedPosition(120);
        motorState.setSecondsToFinish(new BigDecimal("1.0"));

        Progress progress = new Progress(500, motorState, CLOCK);
        progress.setStartTimeMillis(NOW - 5000L);

        progress.update();

        assertEquals(100, motorState.getCurrentPosition());
        assertEquals(0, progress.getLastPosition());
        assertEquals(BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY), motorState.getSecondsToFinish());
    }

    @Test
    void update_doesNotDropBelowLowerBound() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(2);
        motorState.setRequestedPosition(-10);
        motorState.setSecondsToFinish(new BigDecimal("1.0"));

        Progress progress = new Progress(400, motorState, CLOCK);
        progress.setStartTimeMillis(NOW - 4000L);

        progress.update();

        assertEquals(0, motorState.getCurrentPosition());
        assertEquals(0, progress.getLastPosition());
        assertEquals(BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY), motorState.getSecondsToFinish());
    }

    @Test
    void update_skipsWhenSecondsPerStepZeroButMovementRequested() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(5);
        motorState.setRequestedPosition(10);
        motorState.setSecondsToFinish(BigDecimal.ZERO);

        Progress progress = new Progress(250, motorState, CLOCK);

        progress.update();

        assertEquals(5, motorState.getCurrentPosition());
        assertEquals(BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY), motorState.getSecondsToFinish());
    }

    @Test
    void update_doesNotChangeStateWhenEstimatedPositionEqualsLast() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(60);
        motorState.setRequestedPosition(70);
        motorState.setSecondsToFinish(new BigDecimal("20.0"));

        Progress progress = new Progress(1000, motorState, CLOCK);
        progress.setLastPosition(60);
        progress.setStartTimeMillis(NOW);

        progress.update();

        assertEquals(60, motorState.getCurrentPosition());
        assertEquals(60, progress.getLastPosition());
    }

    @Test
    void update_ignoresEstimatedPositionAboveHundred() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(95);
        motorState.setRequestedPosition(100);
        motorState.setSecondsToFinish(new BigDecimal("5.0"));

        Progress progress = new Progress(100, motorState, CLOCK);
        progress.setStartTimeMillis(NOW - 10000L);

        progress.update();

        assertEquals(100, motorState.getCurrentPosition());
        assertEquals(0, progress.getLastPosition());
        assertEquals(BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY), motorState.getSecondsToFinish());
    }

    @Test
    void update_ignoresEstimatedPositionBelowZero() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(5);
        motorState.setRequestedPosition(0);
        motorState.setSecondsToFinish(new BigDecimal("5.0"));

        Progress progress = new Progress(100, motorState, CLOCK);
        progress.setStartTimeMillis(NOW - 10000L);

        progress.update();

        assertEquals(0, motorState.getCurrentPosition());
        assertEquals(0, progress.getLastPosition());
        assertEquals(BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY), motorState.getSecondsToFinish());
    }

    @Test
    void gettersExposeConstructorValues() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(0);
        motorState.setRequestedPosition(100);
        motorState.setSecondsToFinish(new BigDecimal("12.5"));

        Progress progress = new Progress(333, motorState, CLOCK);

        assertEquals(0, new BigDecimal("333").compareTo(progress.getIntervalMillis()));
        assertEquals(0, new BigDecimal("12500").compareTo(progress.getStartMillisToFinish()));
        assertEquals(0, new BigDecimal("125.00").compareTo(progress.getMillisPerStep()));
        assertEquals(BigDecimal.valueOf(0), progress.getStartPosition());
        assertNotNull(progress.getStartTimeMillis());
    }

    @Test
    void toStringProducesJson() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(0);
        motorState.setRequestedPosition(0);
        motorState.setSecondsToFinish(BigDecimal.ZERO);

        Progress progress = new Progress(100, motorState, CLOCK);

        String json = progress.toString();

        assertTrue(json.contains("\"intervalMillis\""));
        assertTrue(json.contains("\"state\""));
    }

    @Test
    void prettyStringIndentsFormattedJson() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(0);
        motorState.setRequestedPosition(0);
        motorState.setSecondsToFinish(BigDecimal.ZERO);

        Progress progress = new Progress(100, motorState, CLOCK);

        String pretty = progress.prettyString();

        assertTrue(pretty.lines().allMatch(line -> line.startsWith("           ")));
    }

    @Test
    void update_whenDirectionStop_keepsStateUnchanged() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(25);
        motorState.setRequestedPosition(25);
        motorState.setSecondsToFinish(new BigDecimal("5.0"));

        Progress progress = new Progress(500, motorState, CLOCK);

        progress.update();

        assertEquals(25, motorState.getCurrentPosition());
        assertEquals(BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY), motorState.getSecondsToFinish());
    }

    @Test
    void update_movesInConfiguredDirection() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(10);
        motorState.setRequestedPosition(20);
        motorState.setSecondsToFinish(new BigDecimal("10.0"));

        Progress progress = new Progress(500, motorState, CLOCK);
        progress.setStartTimeMillis(NOW - 3000L);

        progress.update();

        assertEquals(13, motorState.getCurrentPosition());
        assertEquals(new BigDecimal("7.000"), motorState.getSecondsToFinish());
    }

    @Test
    void update_clampsSecondsToFinishAtZero() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(40);
        motorState.setRequestedPosition(30);
        motorState.setSecondsToFinish(new BigDecimal("5.0"));

        Progress progress = new Progress(500, motorState, CLOCK);
        progress.setStartTimeMillis(NOW - 7000L);

        progress.update();

        assertEquals(BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY), motorState.getSecondsToFinish());
    }

    private static MotorState moving(int from, int to, String secondsToFinish) {
        MotorState motorState = new MotorState(MotorStateCalculator.ValidMotorDirectionState.MOTOR_GO_TO_POSITION);
        motorState.setPower(OnOffToggleStateCalculator.ValidOnOffToggle.ON);
        motorState.setCurrentPosition(from);
        motorState.setRequestedPosition(to);
        motorState.setSecondsToFinish(new BigDecimal(secondsToFinish));
        return motorState;
    }

    private static Progress progress(MotorState motorState, int interval, long elapsedMillis) {
        Progress progress = new Progress(interval, motorState, CLOCK);
        progress.setStartTimeMillis(NOW - elapsedMillis);
        return progress;
    }

    @Test
    void update_whileMovingKeepsPowerOnAndReportsTheDirection() {
        MotorState down = moving(10, 20, "10.0");
        progress(down, 500, 3000L).update();
        assertEquals(OnOffToggleStateCalculator.ValidOnOffToggle.ON, down.getPower());
        assertEquals(MotorStateCalculator.ValidMotorDirectionState.DOWN, down.getState());

        MotorState up = moving(20, 10, "10.0");
        progress(up, 500, 3000L).update();
        assertEquals(OnOffToggleStateCalculator.ValidOnOffToggle.ON, up.getPower());
        assertEquals(MotorStateCalculator.ValidMotorDirectionState.UP, up.getState());
    }

    @Test
    void update_whenTheTimeIsUpFinishesAtTheRequestedPosition() {
        MotorState motorState = moving(10, 20, "10.0");

        progress(motorState, 500, 10_000L).update();

        assertEquals(OnOffToggleStateCalculator.ValidOnOffToggle.OFF, motorState.getPower());
        assertEquals(20, motorState.getCurrentPosition());
        assertEquals(BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY), motorState.getSecondsToFinish());
    }

    @Test
    void update_whenThePositionIsReachedFinishesEvenWithTimeLeft() {
        MotorState motorState = moving(10, 20, "10.0");

        // 9.96 steps rounds up to the requested position, 40 ms is more than the interval
        progress(motorState, 10, 9_960L).update();

        assertEquals(OnOffToggleStateCalculator.ValidOnOffToggle.OFF, motorState.getPower());
        assertEquals(20, motorState.getCurrentPosition());
        assertEquals(BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY), motorState.getSecondsToFinish());
    }

    @Test
    void update_withExactlyOneIntervalLeftKeepsRunning() {
        MotorState motorState = moving(0, 100, "10.0");

        progress(motorState, 500, 9_500L).update();

        assertEquals(OnOffToggleStateCalculator.ValidOnOffToggle.ON, motorState.getPower());
        assertEquals(0, new BigDecimal("0.5").compareTo(motorState.getSecondsToFinish()));
        assertEquals(95, motorState.getCurrentPosition());
    }

    @Test
    void update_withLessThanOneIntervalLeftFinishes() {
        MotorState motorState = moving(0, 100, "10.0");

        progress(motorState, 500, 9_501L).update();

        assertEquals(OnOffToggleStateCalculator.ValidOnOffToggle.OFF, motorState.getPower());
        assertEquals(100, motorState.getCurrentPosition());
    }

    @Test
    void update_followsTheClockOverSeveralUpdates() {
        MotorState motorState = moving(0, 10, "10.0");
        AtomicLong now = new AtomicLong(NOW);
        Progress progress = new Progress(100, motorState, now::get);

        now.addAndGet(2000L);
        progress.update();
        assertEquals(2, motorState.getCurrentPosition());
        assertEquals(0, new BigDecimal("8.0").compareTo(motorState.getSecondsToFinish()));

        now.addAndGet(5000L);
        progress.update();
        assertEquals(7, motorState.getCurrentPosition());
        assertEquals(0, new BigDecimal("3.0").compareTo(motorState.getSecondsToFinish()));
        assertEquals(OnOffToggleStateCalculator.ValidOnOffToggle.ON, motorState.getPower());

        now.addAndGet(3000L);
        progress.update();
        assertEquals(10, motorState.getCurrentPosition());
        assertEquals(OnOffToggleStateCalculator.ValidOnOffToggle.OFF, motorState.getPower());
    }

    @Test
    void stoppedMotorStaysWhereItIsEvenWithAnotherPositionRequested() {
        MotorState motorState = moving(30, 80, "10.0");
        motorState.setState(MotorStateCalculator.ValidMotorDirectionState.STOP);

        Progress progress = progress(motorState, 500, 3000L);
        progress.update();

        assertSame(Progress.Direction.STOP, progress.getDirection());
        assertEquals(OnOffToggleStateCalculator.ValidOnOffToggle.OFF, motorState.getPower());
        assertEquals(30, motorState.getCurrentPosition());
        assertEquals(MotorStateCalculator.ValidMotorDirectionState.STOP, motorState.getState());
    }

    @Test
    void alreadyAtThePositionNeedsNoTimeToFinish() {
        // An event may report a position without the bytes for the time left
        MotorState motorState = moving(30, 30, "0");
        motorState.setSecondsToFinish(null);

        new Progress(500, motorState, CLOCK).update();

        assertEquals(OnOffToggleStateCalculator.ValidOnOffToggle.OFF, motorState.getPower());
        assertEquals(30, motorState.getCurrentPosition());
    }

    @Test
    void movingWithoutATimeOrPositionCannotBeTracked() {
        MotorState noTime = moving(10, 20, "0");
        noTime.setSecondsToFinish(null);
        assertThrows(NullPointerException.class, () -> new Progress(500, noTime, CLOCK));

        MotorState noCurrent = moving(10, 20, "1");
        noCurrent.setCurrentPosition(null);
        assertThrows(NullPointerException.class, () -> new Progress(500, noCurrent, CLOCK));

        MotorState noRequested = moving(10, 20, "1");
        noRequested.setRequestedPosition(null);
        assertThrows(NullPointerException.class, () -> new Progress(500, noRequested, CLOCK));
    }

    @Test
    void constructor_computesIntervalSecondsAndSecondsPerStepWithExpectedPrecision() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(0);
        motorState.setRequestedPosition(100);
        motorState.setSecondsToFinish(new BigDecimal("12.5"));

        Progress progress = new Progress(333, motorState, CLOCK);

        assertEquals(0, new BigDecimal("333").compareTo(progress.getIntervalMillis()));
        assertEquals(0, new BigDecimal("125").compareTo(progress.getMillisPerStep()));
    }
}
