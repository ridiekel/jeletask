package io.github.ridiekel.jeletask.client.spec.state.impl;

import io.github.ridiekel.jeletask.client.spec.state.State;

import java.math.BigDecimal;

public class LuxState extends State<BigDecimal> {
    public LuxState() {
    }

    public LuxState(BigDecimal state) {
        super(state);
    }
}
