package io.github.ridiekel.jeletask.client.spec.component;

import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;

/**
 * A motor, shown as a cover in Home Assistant.
 */
public class MotorSpec extends ComponentSpec {
    @Override
    public Function getFunction() {
        return Function.MOTOR;
    }
}
