package io.github.ridiekel.jeletask.client.spec.component;

import io.github.ridiekel.jeletask.client.spec.Function;

/**
 * A local mood, shown as a switch in Home Assistant unless the type says otherwise.
 */
public class LocalMoodSpec extends OnOffSpec {
    @Override
    public Function getFunction() {
        return Function.LOCMOOD;
    }
}
