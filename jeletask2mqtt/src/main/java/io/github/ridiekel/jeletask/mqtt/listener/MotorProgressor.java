package io.github.ridiekel.jeletask.mqtt.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MotorProgressor extends TimerTask {
    private static final Logger LOG = LoggerFactory.getLogger(MotorProgressor.class);
    private final Map<ComponentSpec, Progress> runningMotors = new ConcurrentHashMap<>();
    private final MqttProcessor processor;

    public MotorProgressor(MqttProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void run() {
        if (!this.runningMotors.isEmpty()) {
            LOG.debug("Running motors (numbers): {}", this.runningMotors.keySet().stream().map(ComponentSpec::getNumber).map(String::valueOf).collect(Collectors.joining(", ")));
            this.runningMotors.forEach(this::publish);
        }
    }

    public void update(ComponentSpec motor, ComponentState state) {
        if (Objects.equals(state.getState(), "ON")) {
            try {
                Progress progress = new Progress(this.processor.getConfiguration().getPublish().getMotorPositionInterval(), state);
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Motor {} started running:\n{}", motor.getNumber(), progress.prettyString());
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("Motor {} started running: {}", motor.getNumber(), progress.getState().getCurrentPosition());
                }
                this.runningMotors.put(motor, progress);
            } catch (Exception e) {
                LOG.debug("Motor {} started running, but exception occured in calculating progress", motor.getNumber(), e);
            }
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Motor {} stopped: \n{}", motor.getNumber(), state.prettyString());
            } else if (LOG.isDebugEnabled()) {
                LOG.debug("Motor {} stopped", motor.getNumber());
            }
            this.runningMotors.remove(motor);
        }
    }

    public void publish(ComponentSpec motor, Progress progress) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Updating motor progress for motor: {}\n{}", motor.getNumber(), progress.prettyString());
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Updating motor position for motor: {}\n{}", motor.getNumber(), progress.getState().getCurrentPosition());
        }
        progress.update();
        this.processor.publishState(motor, progress.getState());
        if ((float) progress.getState().getSecondsToFinish() == 0) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Motor {} reached the end: \n{}", motor.getNumber(), progress.prettyString());
            } else if (LOG.isDebugEnabled()) {
                LOG.trace("Motor {} reached the end", motor.getNumber());
            }
            this.runningMotors.remove(motor);
        }
    }

    private static class Progress {
        private final Integer intervalMilliseconds;
        private final ComponentState state;
        private final long startTimeMillis;
        private final float startSecondsToFinish;
        private final float secondsPerStep;
        private final int startPosition;
        private int lastPosition = 0;

        public Progress(Integer intervalMilliseconds, ComponentState state) {
            this.intervalMilliseconds = intervalMilliseconds;
            this.state = state;
            this.secondsPerStep = state.getSecondsToFinish().floatValue() / Math.abs(state.getPosition().intValue() - state.getCurrentPosition().intValue());
            this.startTimeMillis = System.currentTimeMillis();
            this.startPosition = state.getCurrentPosition().intValue();
            this.startSecondsToFinish = state.getSecondsToFinish().floatValue();
        }

        public void update() {
            try {
                float estimatedPositionChange = (System.currentTimeMillis() - this.startTimeMillis) / this.secondsPerStep / 1000F;
                int estimatedPosition = "UP".equalsIgnoreCase(this.state.getLastDirection()) ? Math.round(this.startPosition - estimatedPositionChange) : Math.round(this.startPosition + estimatedPositionChange);
                if (estimatedPosition != this.lastPosition && (estimatedPosition >= 0 && estimatedPosition <= 100)) {
                    this.state.setCurrentPosition(estimatedPosition);
                    this.lastPosition = estimatedPosition;
                }
                float newSecondsToFinish = this.startSecondsToFinish - ((System.currentTimeMillis() - this.startTimeMillis) / 1000F);
                if (newSecondsToFinish > 0) {
                    if (newSecondsToFinish < this.intervalMilliseconds / 1000F) {
                        newSecondsToFinish = 0;
                    }
                    this.state.setSecondsToFinish(newSecondsToFinish);
                }
            } catch (Exception e) {
                LOG.warn(e.getMessage());
            }
        }

        public ComponentState getState() {
            return state;
        }

        @Override
        public String toString() {
            try {
                return ComponentState.OBJECT_MAPPER.writeValueAsString(this);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        public String prettyString() {
            try {
                return ComponentState.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this).lines().map(l -> "           " + l).collect(Collectors.joining("\n"));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
