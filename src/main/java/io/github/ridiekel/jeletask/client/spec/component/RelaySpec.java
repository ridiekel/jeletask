package io.github.ridiekel.jeletask.client.spec.component;

import io.github.ridiekel.jeletask.client.spec.Function;

/**
 * A relay, shown as a light in Home Assistant unless the type says otherwise.
 */
public class RelaySpec extends OnOffSpec {
    @Override
    public Function getFunction() {
        return Function.RELAY;
    }
}
