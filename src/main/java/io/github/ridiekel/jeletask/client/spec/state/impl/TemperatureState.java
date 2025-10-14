package io.github.ridiekel.jeletask.client.spec.state.impl;

import io.github.ridiekel.jeletask.client.spec.state.State;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
public class TemperatureState extends State<BigDecimal> {
    public TemperatureState(BigDecimal state) {
        super(state);
    }
}
