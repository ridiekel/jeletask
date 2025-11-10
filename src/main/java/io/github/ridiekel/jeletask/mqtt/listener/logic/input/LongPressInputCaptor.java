package io.github.ridiekel.jeletask.mqtt.listener.logic.input;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.InputStateCalculator;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.InputState;
import io.github.ridiekel.jeletask.mqtt.listener.MqttPublisher;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LongPressInputCaptor {
    private static final Logger LOG = LogManager.getLogger();

    private final Map<ComponentSpec, Captor> pressedInputs = new ConcurrentHashMap<>();
    private final MqttPublisher processor;

    public LongPressInputCaptor(MqttPublisher processor) {
        this.processor = processor;
    }

    public void update(ComponentSpec component) {
        InputStateCalculator.ValidInputState state = (InputStateCalculator.ValidInputState) component.getState().getState();
        if (Objects.equals(state, InputStateCalculator.ValidInputState.CLOSED)) {
            startPress(component);
        } else {
            stopPress(component);
        }
    }

    public void stopPress(ComponentSpec component) {
        Captor captor = getCaptor(component);

        if (captor.getStartPressTime() == null) {
            logConditional("IGNORED_STOP", component, captor);
            return;
        }

        logConditional("STOPPED", component, captor);
        captor.stopPress();
        this.publishPressEvent(component, captor.createSnapshot());
        this.publishResetState(component, captor);
    }

    private void publishPressEvent(ComponentSpec input, Captor captor) {
        logConditional("PRESS_EVENT", input, captor);
        this.processor.publishState(input, captor);
    }

    private void publishResetState(ComponentSpec input, Captor captor) {
        Captor.EXECUTOR_SERVICE.schedule(() -> {
            Captor resetState = new Captor(captor.getLongPressConfigInMillis());

            logConditional("STATE_RESET", input, resetState);
            this.processor.publishState(input, resetState);

            captor.reset();
        }, 50, TimeUnit.MILLISECONDS);
    }


    public void startPress(ComponentSpec component) {
        Captor captor = this.getCaptor(component);
        captor.startPress(() -> this.stopPress(component));
        logConditional("STARTED", component, captor);
    }

    private void logConditional(String action, ComponentSpec input, Captor captor) {
        if (LOG.isTraceEnabled()) {
            log(LOG::trace, action, input, "\n" + captor.prettyString());
        } else if (LOG.isDebugEnabled()) {
            log(LOG::debug, action, input, captor.toString());
        } else if (LOG.isInfoEnabled()) {
            log(LOG::info, action, input, captor.toString());
        }
    }

    private void log(Consumer<String> level, String what, String message) {
        level.accept(AnsiOutput.toString(AnsiColor.BRIGHT_YELLOW, "[" + StringUtils.rightPad("PRESS", 10) + "] - [" + StringUtils.rightPad(what, 10) + "] - ", AnsiColor.GREEN, message, AnsiColor.DEFAULT));
    }

    private void log(Consumer<String> level, String what, ComponentSpec motor, String message) {
        log(level, what, AnsiOutput.toString(AnsiColor.BRIGHT_YELLOW, "[" + StringUtils.rightPad("INPUT", 10) + "] - [" + StringUtils.leftPad(String.valueOf(motor.getNumber()), 3) + "] - [" + StringUtils.leftPad(motor.getDescription(), 40) + "] - ", AnsiColor.GREEN, message, AnsiColor.DEFAULT));
    }

    private Captor getCaptor(ComponentSpec component) {
        return this.pressedInputs.computeIfAbsent(component, c -> new Captor(component.getLong_press_duration_millis()));
    }

    static class Captor extends InputState {
        @Getter
        private Long stopPressTime;
        @Getter
        private Long startPressTime;
        private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);
        private static final ScheduledExecutorService EXECUTOR_SERVICE =
                Executors.newScheduledThreadPool(4, r -> {
                    Thread t = new Thread(r);
                    t.setName("button-press-" + THREAD_COUNTER.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                });
        @JsonIgnore
        private List<RunningTimer> timers = new ArrayList<>();

        private final long longPressConfigInMillis;

        public Captor(long longPressConfigInMillis) {
            this.longPressConfigInMillis = longPressConfigInMillis;
        }

        public void stopPress() {
            this.stopPressTime = System.currentTimeMillis();
            this.timers.forEach(RunningTimer::stop);
            this.timers.clear();
        }

        public void startPress(Runnable onTimeout) {
            this.startPressTime = System.currentTimeMillis();
            RunningTimer runningTimer = new RunningTimer(onTimeout);
            EXECUTOR_SERVICE.schedule(runningTimer, longPressConfigInMillis, TimeUnit.MILLISECONDS);
            this.timers.add(runningTimer);
        }

        public Captor createSnapshot() {
            Captor snapshot = new Captor(this.longPressConfigInMillis);
            snapshot.startPressTime = this.startPressTime;
            snapshot.stopPressTime = this.stopPressTime;
            // Timers list is intentionally NOT copied (not needed for publication)
            return snapshot;
        }

        public Long getLongPressConfigInMillis() {
            return longPressConfigInMillis;
        }

        @Override
        public InputStateCalculator.ValidInputState getState() {
            return this.getStopPressTime() == null ? InputStateCalculator.ValidInputState.NOT_PRESSED : getActualPressDuration();
        }

        private InputStateCalculator.ValidInputState getActualPressDuration() {
            return this.getPressDurationMillis() >= this.getLongPressConfigInMillis() ? InputStateCalculator.ValidInputState.LONG_PRESS : InputStateCalculator.ValidInputState.SHORT_PRESS;
        }

        public Long getPressDurationMillis() {
            return this.getStopPressTime() == null ? -1 : this.getStopPressTime() - this.getStartPressTime();
        }

        @Override
        public String toString() {
            try {
                return State.OBJECT_MAPPER.writeValueAsString(this);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public String prettyString() {
            try {
                return State.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this).lines().map(l -> "           " + l).collect(Collectors.joining("\n"));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        }

        public void reset() {
            this.startPressTime = null;
            this.stopPressTime = null;
            this.timers.clear();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            Captor captor = (Captor) o;

            return new EqualsBuilder().appendSuper(super.equals(o)).append(longPressConfigInMillis, captor.longPressConfigInMillis).append(stopPressTime, captor.stopPressTime).append(startPressTime, captor.startPressTime).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(stopPressTime).append(startPressTime).append(longPressConfigInMillis).toHashCode();
        }
    }

    private static class RunningTimer implements Runnable {
        private final Runnable onTimeout;
        private boolean running;

        public RunningTimer(Runnable onTimeout) {
            this.onTimeout = onTimeout;
            this.running = true;
        }

        public void stop() {
            this.running = false;
        }

        @Override
        public void run() {
            if (this.running) {
                this.onTimeout.run();
            }
        }
    }
}

