package io.github.ridiekel.jeletask.client.spec.component;

import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;

/**
 * A condition, read only and shown as a binary sensor in Home Assistant.
 */
public class ConditionSpec extends ComponentSpec {
    @Override
    public Function getFunction() {
        return Function.COND;
    }
}
