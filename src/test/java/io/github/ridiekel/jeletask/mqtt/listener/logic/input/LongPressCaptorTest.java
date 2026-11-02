package io.github.ridiekel.jeletask.mqtt.listener.logic.input;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.InputStateCalculator.ValidInputState;
import io.github.ridiekel.jeletask.client.spec.state.impl.InputState;
import io.github.ridiekel.jeletask.mqtt.listener.logic.input.LongPressInputCaptor.Captor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The state that is published for a button: NOT_PRESSED until released, then SHORT_PRESS or LONG_PRESS by duration.
 * The timeouts are far away, so no timer fires during these tests.
 */
class LongPressCaptorTest {
    private static final long FAR_AWAY = 60_000;

    private static void nothing() {
        // The long press timeout is not part of these tests
    }

    @Test
    void notPressedUntilReleased() {
        Captor captor = new Captor(FAR_AWAY);

        assertThat(captor.getState()).isEqualTo(ValidInputState.NOT_PRESSED);
        assertThat(captor.getPressDurationMillis()).isEqualTo(-1L);

        captor.startPress(LongPressCaptorTest::nothing);

        assertThat(captor.getStartPressTime()).isNotNull();
        assertThat(captor.getState()).isEqualTo(ValidInputState.NOT_PRESSED);
        assertThat(captor.getPressDurationMillis()).isEqualTo(-1L);
    }

    @Test
    void aReleaseBeforeTheConfiguredDurationIsAShortPress() {
        Captor captor = new Captor(FAR_AWAY);
        captor.startPress(LongPressCaptorTest::nothing);

        captor.stopPress();

        assertThat(captor.getState()).isEqualTo(ValidInputState.SHORT_PRESS);
        assertThat(captor.getPressDurationMillis()).isBetween(0L, FAR_AWAY - 1);
    }

    @Test
    void reachingTheConfiguredDurationIsALongPress() {
        // A duration of 0 ms is reached by every release
        Captor captor = new Captor(0);
        captor.startPress(LongPressCaptorTest::nothing);

        captor.stopPress();

        assertThat(captor.getState()).isEqualTo(ValidInputState.LONG_PRESS);
    }

    @Test
    void aSnapshotKeepsTheTimesAndSurvivesAReset() {
        Captor captor = new Captor(FAR_AWAY);
        captor.startPress(LongPressCaptorTest::nothing);
        captor.stopPress();

        Captor snapshot = captor.createSnapshot();
        captor.reset();

        assertThat(snapshot.getState()).isEqualTo(ValidInputState.SHORT_PRESS);
        assertThat(snapshot.getLongPressConfigInMillis()).isEqualTo(FAR_AWAY);
        assertThat(captor.getStartPressTime()).isNull();
        assertThat(captor.getStopPressTime()).isNull();
        assertThat(captor.getState()).isEqualTo(ValidInputState.NOT_PRESSED);
    }

    @Test
    void equalWhenConfigAndTimesAreEqual() {
        Captor pressed = new Captor(FAR_AWAY);
        pressed.startPress(LongPressCaptorTest::nothing);
        pressed.stopPress();
        Object plainState = new InputState(ValidInputState.SHORT_PRESS);

        assertThat(pressed)
                .isEqualTo(pressed)
                .isEqualTo(pressed.createSnapshot())
                .hasSameHashCodeAs(pressed.createSnapshot())
                .isNotEqualTo(new Captor(FAR_AWAY))
                .isNotEqualTo(null)
                // Same state, but a plain InputState is no captor
                .isNotEqualTo(plainState);
        assertThat(new Captor(500))
                .isEqualTo(new Captor(500))
                .hasSameHashCodeAs(new Captor(500))
                .isNotEqualTo(new Captor(501));
    }

    @Test
    void theJsonHoldsStateAndDurationButNoTimers() {
        // This is what ends up as MQTT state payload of the input
        assertThat(new Captor(500)).hasToString(
                "{\"state\":\"NOT_PRESSED\",\"long_press_config_in_millis\":500,\"press_duration_millis\":-1}");
    }

    @Test
    void thePrettyJsonIsIndentedForTheLog() {
        String pretty = new Captor(500).prettyString();

        assertThat(pretty.lines()).hasSizeGreaterThan(1).allSatisfy(line -> assertThat(line).startsWith("           "));
        assertThat(pretty).contains("\"state\" : \"NOT_PRESSED\"").doesNotContain("timers");
    }
}
