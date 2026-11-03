package io.github.ridiekel.jeletask.client.spec.component;

import io.github.ridiekel.jeletask.client.spec.Function;

/**
 * A timed mood, shown as a switch in Home Assistant unless the type says otherwise.
 */
public class TimedMoodSpec extends OnOffSpec {
    @Override
    public Function getFunction() {
        return Function.TIMEDMOOD;
    }
}
