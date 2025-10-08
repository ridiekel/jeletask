package io.github.ridiekel.jeletask.client.spec.state.impl;

import io.github.ridiekel.jeletask.client.spec.state.State;

import java.math.BigDecimal;

public class HumidityState extends State<BigDecimal> {
    public HumidityState() {
    }

    public HumidityState(BigDecimal state) {
        super(state);
    }
}
