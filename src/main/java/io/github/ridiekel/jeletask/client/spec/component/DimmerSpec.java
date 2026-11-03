package io.github.ridiekel.jeletask.client.spec.component;

import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;

/**
 * A dimmer, shown as a light in Home Assistant.
 */
public class DimmerSpec extends ComponentSpec {
    @Override
    public Function getFunction() {
        return Function.DIMMER;
    }
}
