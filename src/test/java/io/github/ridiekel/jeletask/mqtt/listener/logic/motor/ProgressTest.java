package io.github.ridiekel.jeletask.mqtt.listener.logic.motor;

import io.github.ridiekel.jeletask.client.spec.state.impl.MotorState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class ProgressTest {

    @Test
    void update_doesNothingWhenDirectionStop() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(25);
        motorState.setRequestedPosition(25);
        motorState.setSecondsToFinish(new BigDecimal("5.0"));

        Progress progress = new Progress(500, motorState);

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

        Progress progress = new Progress(500, motorState);
        progress.setStartTimeMillis(System.currentTimeMillis() - 3000L);

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

        Progress progress = new Progress(200, motorState);
        progress.setStartTimeMillis(System.currentTimeMillis() - 2000L);

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

        Progress progress = new Progress(250, motorState);
        progress.setStartTimeMillis(System.currentTimeMillis() - 9000L);

        progress.update();

        assertEquals(BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY), motorState.getSecondsToFinish());
    }

    @Test
    void update_setsSecondsToFinishZeroWhenLessThanInterval() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(10);
        motorState.setRequestedPosition(11);
        motorState.setSecondsToFinish(new BigDecimal("1.0"));

        Progress progress = new Progress(200, motorState);
        progress.setStartTimeMillis(System.currentTimeMillis() - 850L);

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

        Progress progress = new Progress(500, motorState);
        progress.setStartTimeMillis(System.currentTimeMillis() - 5000L);

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

        Progress progress = new Progress(400, motorState);
        progress.setStartTimeMillis(System.currentTimeMillis() - 4000L);

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

        Progress progress = new Progress(250, motorState);

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

        Progress progress = new Progress(1000, motorState);
        progress.setLastPosition(60);
        progress.setStartTimeMillis(System.currentTimeMillis());

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

        Progress progress = new Progress(100, motorState);
        progress.setStartTimeMillis(System.currentTimeMillis() - 10000L);

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

        Progress progress = new Progress(100, motorState);
        progress.setStartTimeMillis(System.currentTimeMillis() - 10000L);

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

        Progress progress = new Progress(333, motorState);

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

        Progress progress = new Progress(100, motorState);

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

        Progress progress = new Progress(100, motorState);

        String pretty = progress.prettyString();

        assertTrue(pretty.lines().allMatch(line -> line.startsWith("           ")));
    }

    @Test
    void update_whenDirectionStop_keepsStateUnchanged() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(25);
        motorState.setRequestedPosition(25);
        motorState.setSecondsToFinish(new BigDecimal("5.0"));

        Progress progress = new Progress(500, motorState);

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

        Progress progress = new Progress(500, motorState);
        progress.setStartTimeMillis(System.currentTimeMillis() - 3000L);

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

        Progress progress = new Progress(500, motorState);
        progress.setStartTimeMillis(System.currentTimeMillis() - 7000L);

        progress.update();

        assertEquals(BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY), motorState.getSecondsToFinish());
    }

    @Test
    void constructor_computesIntervalSecondsAndSecondsPerStepWithExpectedPrecision() {
        MotorState motorState = new MotorState();
        motorState.setCurrentPosition(0);
        motorState.setRequestedPosition(100);
        motorState.setSecondsToFinish(new BigDecimal("12.5"));

        Progress progress = new Progress(333, motorState);

        assertEquals(0, new BigDecimal("333").compareTo(progress.getIntervalMillis()));
        assertEquals(0, new BigDecimal("125").compareTo(progress.getMillisPerStep()));
    }
}
