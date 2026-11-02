package io.github.ridiekel.jeletask.mockserver;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.MotorStateCalculator.ValidMotorDirectionState;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator.ValidOnOffToggle;
import io.github.ridiekel.jeletask.client.spec.state.impl.MotorState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TeletaskMockServerMotorRunTest {
    private static final long START = 1_000_000;

    @Test
    void halfwayARunTheMotorIsStillMovingWithHalfTheTimeLeft() {
        MotorState state = run(ValidOnOffToggle.ON, 0, 100, "5.000").at(START + 2500);

        assertThat(state.getState()).isEqualTo(ValidMotorDirectionState.DOWN);
        assertThat(state.getPower()).isEqualTo(ValidOnOffToggle.ON);
        assertThat(state.getCurrentPosition()).isEqualTo(50);
        assertThat(state.getRequestedPosition()).isEqualTo(100);
        assertThat(state.getSecondsToFinish()).isEqualByComparingTo("2.5");
    }

    @Test
    void afterTheRunTheMotorStandsStillAtTheRequestedPosition() {
        MotorState state = run(ValidOnOffToggle.ON, 0, 100, "5.000").at(START + 5000);

        assertThat(state.getState()).isEqualTo(ValidMotorDirectionState.STOP);
        assertThat(state.getPower()).isEqualTo(ValidOnOffToggle.OFF);
        assertThat(state.getCurrentPosition()).isEqualTo(100);
    }

    @Test
    void aStoppedMotorStaysWhereItWas() {
        MotorState state = run(ValidOnOffToggle.OFF, 30, null, null).at(START + 100);

        assertThat(state.getState()).isEqualTo(ValidMotorDirectionState.STOP);
        assertThat(state.getPower()).isEqualTo(ValidOnOffToggle.OFF);
        assertThat(state.getCurrentPosition()).isEqualTo(30);
    }

    private static TeletaskMockServer.MotorRun run(ValidOnOffToggle power, Integer from, Integer to, String seconds) {
        MotorState response = new MotorState(ValidMotorDirectionState.DOWN);
        response.setPower(power);
        response.setCurrentPosition(from);
        response.setRequestedPosition(to);
        response.setSecondsToFinish(seconds == null ? null : new BigDecimal(seconds));
        return new TeletaskMockServer.MotorRun(response, START);
    }
}
