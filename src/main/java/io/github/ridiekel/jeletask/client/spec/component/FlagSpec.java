package io.github.ridiekel.jeletask.client.spec.component;

import io.github.ridiekel.jeletask.client.spec.Function;

/**
 * A flag, shown as a switch in Home Assistant unless the type says otherwise.
 */
public class FlagSpec extends OnOffSpec {
    @Override
    public Function getFunction() {
        return Function.FLAG;
    }
}
