package io.github.ridiekel.jeletask.mqtt.listener;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.MotorState;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;

import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MotorProgressor extends TimerTask {
    private static final Logger LOG = LogManager.getLogger();

    private final Map<ComponentSpec, Progress> runningMotors = new ConcurrentHashMap<>();
    private final Integer interval;
    private final BiConsumer<ComponentSpec, State<?>> processor;

    public MotorProgressor(Integer interval, BiConsumer<ComponentSpec, State<?>> processor) {
        this.interval = interval;
        this.processor = processor;
    }

    @Override
    public void run() {
        if (!this.runningMotors.isEmpty()) {
            log(LOG::trace, "RUNNING", this.runningMotors.keySet().stream().map(c -> "{" + c.getNumber() + ": " + c.getDescription() + "}").collect(Collectors.joining(", ")));
            this.runningMotors.forEach(this::publish);
        }
    }

    private void log(Consumer<String> level, String what, String message) {
        level.accept(AnsiOutput.toString(AnsiColor.BRIGHT_YELLOW, "[" + StringUtils.rightPad("PROGRESS", 10) + "] - [" + StringUtils.rightPad(what, 10) + "] - ", AnsiColor.GREEN, message, AnsiColor.DEFAULT));
    }

    private void log(Consumer<String> level, String what, ComponentSpec motor, String message) {
        log(level, what, AnsiOutput.toString(AnsiColor.BRIGHT_YELLOW, "[" + StringUtils.rightPad("MOTOR", 10) + "] - [" + StringUtils.leftPad(String.valueOf(motor.getNumber()), 3) + "] - [" + StringUtils.leftPad(motor.getDescription(), 40) + "] - ", AnsiColor.GREEN, message, AnsiColor.DEFAULT));
    }

    public void update(ComponentSpec motor) {
        MotorState state = (MotorState) motor.getState();
        if (OnOffToggleStateCalculator.ValidOnOffToggle.ON.equals(state.getPower())) {
            try {
                Progress progress = new Progress(this.interval, state);
                logConditional("STARTED", motor, progress::prettyString, progress::toString, progress.getState());
                this.runningMotors.put(motor, progress);
            } catch (Exception e) {
                String message = "Exception in calculating progress: " + e.getMessage();
                log(LOG::debug, "STARTED", motor, message);
                LOG.trace(message, e);
            }
        } else {
            removeMotor(motor);
        }
    }

    private void removeMotor(ComponentSpec motor) {
        MotorState state = (MotorState) motor.getState();
        logConditional("STOPPED", motor, state::prettyString, state::toString, state);
        this.runningMotors.remove(motor);
    }

    private void logConditional(String action, ComponentSpec motor, Supplier<String> traceState, Supplier<String> debugState, MotorState state) {
        if (LOG.isTraceEnabled()) {
            log(LOG::trace, action, motor, "\n" + traceState.get());
        } else if (LOG.isDebugEnabled()) {
            log(LOG::debug, action, motor, debugState.get());
        } else if (LOG.isInfoEnabled()) {
            log(LOG::info, action, motor, state.toString());
        }
    }

    private void publish(ComponentSpec motor, Progress progress) {
        progress.update();
        MotorState state = progress.getState();
        logConditional("UPDATE", motor, progress::prettyString, progress::toString, state);
        this.processor.accept(motor, state);
        if (OnOffToggleStateCalculator.ValidOnOffToggle.OFF.equals(state.getPower())) {
            removeMotor(motor);
        }
    }
}
