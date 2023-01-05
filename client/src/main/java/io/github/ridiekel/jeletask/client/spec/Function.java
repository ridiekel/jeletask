package io.github.ridiekel.jeletask.client.spec;

import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

import java.io.Serializable;

/**
 * Define all Teletask functions used by the API here.
 */
public enum Function {
    RELAY("relay", state -> true),
    DIMMER("dimmer", state -> true),
    MOTOR("motor on/off", state -> !"STOP".equalsIgnoreCase(state.getState())),
    LOCMOOD("local mood", state -> true),
    TIMEDMOOD("timed mood", state -> true),
    GENMOOD("general mood", state -> true),
    FLAG("flag", state -> true),
    SENSOR("sensor value", state -> true),
    COND("condition", state -> true),
    INPUT("input", state -> true),
    TIMEDFNC("timed function", state -> true),
    DISPLAYMESSAGE("Display message", state -> false, false);
    

    private final String description;
    private final ShouldReceiveAcknowledge shouldReceiveAcknowledge;
    private final boolean includeInGroupGet;

    Function(String description, ShouldReceiveAcknowledge shouldReceiveAcknowledge) {
        this(description, shouldReceiveAcknowledge, true);
    }

    Function(String description, ShouldReceiveAcknowledge shouldReceiveAcknowledge, boolean includeInGroupGet) {
        this.description = description;
        this.shouldReceiveAcknowledge = shouldReceiveAcknowledge;
        this.includeInGroupGet = includeInGroupGet;
    }

    public boolean shouldReceiveAcknowledge(ComponentState state) {
        return this.shouldReceiveAcknowledge.test(state);
    }

    public String getDescription() {
        return this.description;
    }

    public boolean isIncludeInGroupGet() {
        return includeInGroupGet;
    }

    @FunctionalInterface
    public interface ShouldReceiveAcknowledge extends Serializable {
        boolean test(ComponentState state);
    }
}
