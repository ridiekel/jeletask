package io.github.ridiekel.jeletask.client.spec.component;

import io.github.ridiekel.jeletask.client.spec.Function;

/**
 * A timed function, shown as a switch in Home Assistant unless the type says otherwise.
 */
public class TimedFunctionSpec extends OnOffSpec {
    @Override
    public Function getFunction() {
        return Function.TIMEDFNC;
    }
}
