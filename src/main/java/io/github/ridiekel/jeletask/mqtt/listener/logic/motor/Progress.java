package io.github.ridiekel.jeletask.mqtt.listener.logic.motor;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.MotorStateCalculator;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.MotorState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;

public class Progress {
    private static final Logger LOG = LogManager.getLogger();

    public static final BigDecimal THOUSAND = new BigDecimal(1000);
    private final BigDecimal intervalMillis;
    private BigDecimal startMillisToFinish;
    private final MotorState state;
    private final BigDecimal millisPerStep;
    private Long startTimeMillis;

    public Progress(Integer intervalMilliseconds, MotorState state) {
        this.intervalMillis = BigDecimal.valueOf(intervalMilliseconds.longValue());
        this.state = state;
        this.startPosition = BigDecimal.valueOf(state.getCurrentPosition());
        int positionDifference = Math.abs(state.getRequestedPosition() - state.getCurrentPosition());
        if (positionDifference == 0 || state.getState() == MotorStateCalculator.ValidMotorDirectionState.STOP) {
            this.millisPerStep = BigDecimal.ZERO;
            this.direction = Direction.STOP;
        } else {
            this.millisPerStep = state.getSecondsToFinish().multiply(THOUSAND)
                    .divide(BigDecimal.valueOf(positionDifference), 2, RoundingMode.HALF_UP);
            this.direction = startPosition.intValue() > state.getRequestedPosition()
                    ? Direction.UP : Direction.DOWN;
            this.startMillisToFinish = state.getSecondsToFinish().multiply(THOUSAND);
        }
        this.startTimeMillis = System.currentTimeMillis();
    }

    private final BigDecimal startPosition;
    private final Direction direction;
    private int lastPosition = 0;

    public void update() {
        if (Direction.STOP == this.direction || BigDecimal.ZERO.compareTo(this.millisPerStep) == 0) {
            this.state.setPower(OnOffToggleStateCalculator.ValidOnOffToggle.OFF);
            this.state.setSecondsToFinish(BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY));
            return;
        }
        long nowMillis = System.currentTimeMillis();
        try {
            BigDecimal elapsedMillis = BigDecimal.valueOf(nowMillis - this.startTimeMillis);
            BigDecimal estimatedStepsMoved = elapsedMillis
                    .divide(this.millisPerStep, 2, RoundingMode.HALF_UP);
            int estimatedPosition = (Direction.UP == this.direction
                    ? this.startPosition.subtract(estimatedStepsMoved)
                    : this.startPosition.add(estimatedStepsMoved)).setScale(0, RoundingMode.HALF_UP).intValue();
            if (estimatedPosition != this.lastPosition && (estimatedPosition >= 0 && estimatedPosition <= 100)) {
                this.state.setCurrentPosition(estimatedPosition);
                this.lastPosition = estimatedPosition;
            }
            BigDecimal newMillisToFinish = this.startMillisToFinish.subtract(elapsedMillis);
            if (newMillisToFinish.compareTo(BigDecimal.ZERO) > 0) {
                if (newMillisToFinish.compareTo(this.intervalMillis) < 0) {
                    newMillisToFinish = BigDecimal.ZERO;
                }
            } else {
                newMillisToFinish = BigDecimal.ZERO;
            }
            this.state.setSecondsToFinish(newMillisToFinish.divide(THOUSAND, 3, RoundingMode.HALF_UP));
            this.state.setState(this.direction.state);

            if (this.state.getCurrentPosition().equals(this.state.getRequestedPosition()) || this.state.getSecondsToFinish().compareTo(BigDecimal.ZERO) == 0) {
                this.state.setPower(OnOffToggleStateCalculator.ValidOnOffToggle.OFF);
                this.state.setSecondsToFinish(BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY));
                this.state.setCurrentPosition(this.state.getRequestedPosition());
            }
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    public BigDecimal getIntervalMillis() {
        return intervalMillis;
    }

    public MotorState getState() {
        return state;
    }

    @SuppressWarnings("unused")
    public Direction getDirection() {
        return direction;
    }

    @SuppressWarnings("unused")
    public BigDecimal getStartMillisToFinish() {
        return startMillisToFinish;
    }

    @SuppressWarnings("unused")
    public Long getStartTimeMillis() {
        return startTimeMillis;
    }

    @SuppressWarnings("unused")
    public BigDecimal getMillisPerStep() {
        return millisPerStep;
    }

    void setLastPosition(int lastPosition) {
        this.lastPosition = lastPosition;
    }

    @SuppressWarnings("unused")
    public BigDecimal getStartPosition() {
        return startPosition;
    }

    @SuppressWarnings("unused")
    public int getLastPosition() {
        return lastPosition;
    }

    void setStartTimeMillis(Long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    public String prettyString() {
        try {
            return State.OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(this)
                    .lines()
                    .map(l -> "           " + l)
                    .collect(Collectors.joining("\n"));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        try {
            return State.OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public enum Direction {
        UP(MotorStateCalculator.ValidMotorDirectionState.UP), DOWN(MotorStateCalculator.ValidMotorDirectionState.DOWN), STOP(MotorStateCalculator.ValidMotorDirectionState.STOP);

        private final MotorStateCalculator.ValidMotorDirectionState state;

        Direction(MotorStateCalculator.ValidMotorDirectionState state) {
            this.state = state;
        }
    }
}
