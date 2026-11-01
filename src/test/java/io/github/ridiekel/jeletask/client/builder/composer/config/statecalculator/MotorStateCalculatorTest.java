package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.MotorStateCalculator.ValidMotorDirectionState;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.MotorStateCalculator.ValidProtectionState;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator.ValidOnOffToggle;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.MotorState;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class MotorStateCalculatorTest {
    private final MotorStateCalculator calculator = new MotorStateCalculator();
    private final ComponentSpec component = new ComponentSpec();

    @Test
    void correctionsSurviveAnEvent() {
        // TT15132: direction, power, protection, position and 5 extra bytes, so a correction is 1 byte
        MotorState state = new MotorState(ValidMotorDirectionState.DOWN);
        state.setPower(ValidOnOffToggle.ON);
        state.setProtection(ValidProtectionState.ON_OVERRULED_BY_USER);
        state.setRequestedPosition(70);
        state.setCurrentPosition(40);
        state.setSecondsToFinish(new BigDecimal("3.00"));
        state.setCorrectionAtZeroPercentInSeconds(5);
        state.setCorrectionAtHundredPercentInSeconds(7);

        byte[] event = calculator.toEventForTesting(component, state);

        Assertions.assertThat(event).hasSize(9);
        Assertions.assertThat(calculator.fromEvent(component, event)).isEqualTo(state);
    }
}
