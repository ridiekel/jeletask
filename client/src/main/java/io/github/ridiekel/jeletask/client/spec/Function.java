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
    COND("condition", state -> true);

    private final String description;
    private final ShouldReceiveAcknowledge shouldReceiveAcknowledge;

    Function(String description, ShouldReceiveAcknowledge shouldReceiveAcknowledge) {
        this.description = description;
        this.shouldReceiveAcknowledge = shouldReceiveAcknowledge;
    }

    public boolean shouldReceiveAcknowledge(ComponentState state) {
        return this.shouldReceiveAcknowledge.test(state);
    }

    public String getDescription() {
        return this.description;
    }

    @FunctionalInterface
    public interface ShouldReceiveAcknowledge extends Serializable {
        boolean test(ComponentState state);
    }
}
