package io.github.ridiekel.jeletask.mqtt.listener.logic.input;

import ch.qos.logback.classic.Level;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.InputStateCalculator.ValidInputState;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.TestComponents;
import io.github.ridiekel.jeletask.client.spec.component.InputSpec;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.InputState;
import io.github.ridiekel.jeletask.mockserver.LogCapture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * A press logs every step at the most detailed level that is enabled; at trace level with the indented JSON of the captor.
 * The long press duration is far away, so only the release in the test ends the press.
 */
class LongPressInputCaptorLoggingTest {
    private final List<State<?>> published = new CopyOnWriteArrayList<>();
    private final LongPressInputCaptor captor = new LongPressInputCaptor((component, state) -> this.published.add(state));
    private final ComponentSpec button = button();

    private static ComponentSpec button() {
        ComponentSpec button = TestComponents.of(Function.INPUT, new InputState(ValidInputState.CLOSED), 12);
        button.setDescription("Front door");
        ((InputSpec) button).setLongPressDurationMillis(60_000);
        return button;
    }

    private List<String> pressAndRelease(Level level) {
        try (LogCapture log = LogCapture.of(LongPressInputCaptor.class, level)) {
            this.captor.update(this.button);
            this.button.setState(new InputState(ValidInputState.OPEN));
            this.captor.update(this.button);
            // The reset to NOT_PRESSED follows 50 ms after the press event
            await().atMost(Duration.ofSeconds(2)).until(() -> this.published.size() == 2);
            return log.messages(level);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"TRACE", "DEBUG", "INFO"})
    void everyStepOfAPressIsLoggedOnce(String level) {
        List<String> messages = pressAndRelease(Level.toLevel(level));

        assertThat(messages).hasSize(4);
        assertThat(messages.get(0)).contains("STARTED").contains(" 12").contains("Front door");
        assertThat(messages.get(1)).contains("STOPPED");
        assertThat(messages.get(2)).contains("PRESS_EVENT").contains("SHORT_PRESS");
        assertThat(messages.get(3)).contains("STATE_RESET").contains("NOT_PRESSED");
    }

    @Test
    void traceShowsTheCaptorAsIndentedJson() {
        List<String> messages = pressAndRelease(Level.TRACE);

        assertThat(messages.get(2)).contains("\n           {").contains("\"state\" : \"SHORT_PRESS\"");
    }

    @Test
    void debugShowsTheCaptorOnOneLine() {
        List<String> messages = pressAndRelease(Level.DEBUG);

        assertThat(messages.get(2)).doesNotContain("\n").contains("\"state\":\"SHORT_PRESS\"");
    }

    @Test
    void aboveInfoNothingIsLoggedButThePressIsStillPublished() {
        assertThat(pressAndRelease(Level.WARN)).isEmpty();
        assertThat(this.published).<Object>extracting(State::getState).containsExactly(ValidInputState.SHORT_PRESS, ValidInputState.NOT_PRESSED);
    }
}
