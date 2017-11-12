package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;

public class MotorStateCalculator extends MappingStateCalculator {
    public MotorStateCalculator(NumberConverter numberConverter, Number up, Number down, Number stop) {
        super(numberConverter,
                new StateMapping("UP", up, 7),
                new StateMapping("DOWN", down,8),
                new StateMapping("STOP", stop,6)
        );
    }
}
