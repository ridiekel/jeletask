package io.github.ridiekel.jeletask.client.spec.component;

import io.github.ridiekel.jeletask.client.spec.Function;

/**
 * A general mood, shown as a switch in Home Assistant unless the type says otherwise.
 */
public class GeneralMoodSpec extends OnOffSpec {
    @Override
    public Function getFunction() {
        return Function.GENMOOD;
    }
}
