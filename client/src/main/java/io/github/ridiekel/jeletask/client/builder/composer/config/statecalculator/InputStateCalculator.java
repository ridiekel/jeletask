
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

        String state_str = "";
        int state = dataBytes[0]; // For digital input (TDS12117) -> 1 = Pulse?, 2 = Pressed, 3 = Released, 9 = Long pressed?

        // Return the received value by default
        state_str = String.valueOf(dataBytes[0]);

        // TDS12117 ? 2 = pressed (contact closed) / 3 = released (contact open)
        if (state == 2)
            state_str = "CLOSED";
        else if (state == 3)
            state_str = "OPEN";

        return new ComponentState(state_str);
    }

}
