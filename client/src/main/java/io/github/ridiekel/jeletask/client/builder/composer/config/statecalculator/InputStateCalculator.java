
package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;

public class InputStateCalculator extends MappingStateCalculator {

    public InputStateCalculator(NumberConverter numberConverter) {
        super(numberConverter,
                new StateMapping("OPEN", 0),
                new StateMapping("CLOSED", 255)
        );
    }
}
