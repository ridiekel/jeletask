package io.github.ridiekel.jeletask.client.spec.state.impl;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;

public class LogState extends OnOffState {
    public LogState() {
    }

    public LogState(OnOffToggleStateCalculator.ValidOnOffToggle state) {
        super(state);
    }
}
