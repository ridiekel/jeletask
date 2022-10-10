
package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;
import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

public class InputStateCalculator extends SimpleStateCalculator {

    public InputStateCalculator(NumberConverter numberConverter) {
        super(numberConverter);
    }


    @Override
    public ComponentState toComponentState(ComponentSpec component, byte[] dataBytes) {
        boolean closed = (dataBytes[0] & (byte)1) == 1;
        return new ComponentState( closed ? "CLOSED" : "OPEN");
    }

}
