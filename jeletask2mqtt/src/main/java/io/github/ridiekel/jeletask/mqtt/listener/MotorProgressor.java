package io.github.ridiekel.jeletask.mqtt.listener;

import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

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
            this.runningMotors.forEach(this::publish);
        }
    }

    public void update(ComponentSpec motor, ComponentState state) {
        if (Objects.equals(state.getState(), "ON")) {
            this.runningMotors.computeIfPresent(motor, (m, s) -> new Progress(state));
        } else {
            this.runningMotors.remove(motor);
        }
    }

    public void publish(ComponentSpec motor, Progress progress) {
        progress.update();
        this.processor.publishState(motor, progress.getState());
    }

    private class MotorTimer extends Timer {
        private final ComponentSpec component;

        public ComponentSpec getComponent() {
            return this.component;
        }

        public MotorTimer(ComponentSpec component) {
            this.component = component;
        }
    }

    private class Progress {

        private final ComponentState state;
        private long startTimeMillis;
        private float startSecondsToFinish;
        private float secondsPerStep;
        private int startPosition = 0;
        private int lastPosition = 0;

        public Progress(ComponentState state) {
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
                if (newSecondsToFinish > 0)
                    this.state.setSecondsToFinish(newSecondsToFinish);
            } catch (Exception e) {
                LOG.warn(e.getMessage());
            }
        }

        public ComponentState getState() {
            return state;
        }
    }


}
