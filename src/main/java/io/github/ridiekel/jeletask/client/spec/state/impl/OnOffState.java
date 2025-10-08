package io.github.ridiekel.jeletask.client.spec.state.impl;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.spec.state.State;

public class OnOffState extends State<OnOffToggleStateCalculator.ValidOnOffToggle> {
    public OnOffState() {
    }

    public OnOffState(OnOffToggleStateCalculator.ValidOnOffToggle state) {
        super(state);
    }
}
