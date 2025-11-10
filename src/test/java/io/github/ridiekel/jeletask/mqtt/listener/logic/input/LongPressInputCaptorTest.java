package io.github.ridiekel.jeletask.mqtt.listener.logic.input;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.InputStateCalculator;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.InputState;
import io.github.ridiekel.jeletask.mqtt.listener.MqttPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LongPressInputCaptorTest {
    private TestMqttProcessor testProcessor;
    private LongPressInputCaptor captor;
    private ComponentSpec testComponent;

    @BeforeEach
    void setUp() {
        testProcessor = new TestMqttProcessor();
        captor = new LongPressInputCaptor(testProcessor);
        testComponent = new ComponentSpec(Function.INPUT, new InputState(InputStateCalculator.ValidInputState.OPEN), 1);
        testComponent.setDescription("Test Button");
        testComponent.setLong_press_duration_millis(500);
        captor.update(testComponent);
    }

    @Test
    @Timeout(2)
    void testShortPress_PublishesShortPressAndReset() throws InterruptedException {
        testComponent.setState(new InputState(InputStateCalculator.ValidInputState.CLOSED));

        captor.update(testComponent);
        Thread.sleep(100);

        testComponent.setState(new InputState(InputStateCalculator.ValidInputState.OPEN));
        captor.update(testComponent);

        testProcessor.waitForPublications(2, 500);

        List<InputState> published = testProcessor.getPublishedStates();
        assertEquals(2, published.size(), "Should publish SHORT_PRESS and NOT_PRESSED");

        assertEquals(InputStateCalculator.ValidInputState.SHORT_PRESS,
                published.get(0).getState(), "First publication should be SHORT_PRESS");
        assertEquals(InputStateCalculator.ValidInputState.NOT_PRESSED,
                published.get(1).getState(), "Second publication should be NOT_PRESSED (reset)");

        Long pressDuration = ((LongPressInputCaptor.Captor) published.get(0)).getPressDurationMillis();
        assertTrue(pressDuration >= 100 && pressDuration < 500,
                "Press duration should be ~100ms, was: " + pressDuration);
    }

    @Test
    @Timeout(2)
    void testLongPress_AutoTriggersWhenThresholdReached() throws InterruptedException {
        testComponent.setState(new InputState(InputStateCalculator.ValidInputState.CLOSED));
        captor.update(testComponent);

        boolean completed = testProcessor.waitForPublications(2, 1000);
        assertTrue(completed, "Should receive 2 publications within timeout");

        List<InputState> published = testProcessor.getPublishedStates();
        assertEquals(2, published.size(), "Should publish LONG_PRESS and NOT_PRESSED");

        assertEquals(InputStateCalculator.ValidInputState.LONG_PRESS,
                published.get(0).getState(),
                "First publication should be LONG_PRESS, got: " + published.get(0).getState());
        assertEquals(InputStateCalculator.ValidInputState.NOT_PRESSED,
                published.get(1).getState(),
                "Second publication should be NOT_PRESSED, got: " + published.get(1).getState());

        Long pressDuration = ((LongPressInputCaptor.Captor) published.get(0)).getPressDurationMillis();
        assertTrue(pressDuration >= 500,
                "Press duration should be >= 500ms, was: " + pressDuration);
    }

    @Test
    @Timeout(2)
    void testLongPressWithManualRelease_StillPublishesCorrectly() throws InterruptedException {
        testComponent.setState(new InputState(InputStateCalculator.ValidInputState.CLOSED));

        captor.update(testComponent);

        testProcessor.waitForPublications(2, 1000);

        testComponent.setState(new InputState(InputStateCalculator.ValidInputState.OPEN));
        captor.update(testComponent);

        Thread.sleep(100);

        List<InputState> published = testProcessor.getPublishedStates();
        assertEquals(2, published.size(),
                "Should only have LONG_PRESS + NOT_PRESSED (manual release ignored)");
        assertEquals(InputStateCalculator.ValidInputState.LONG_PRESS, published.get(0).getState());
        assertEquals(InputStateCalculator.ValidInputState.NOT_PRESSED, published.get(1).getState());
    }

    @Test
    @Timeout(3)
    void testConsecutivePresses_EachResetsCorrectly() throws InterruptedException {
        testComponent.setState(new InputState(InputStateCalculator.ValidInputState.CLOSED));
        captor.update(testComponent);
        Thread.sleep(100);
        testComponent.setState(new InputState(InputStateCalculator.ValidInputState.OPEN));
        captor.update(testComponent);

        testProcessor.waitForPublications(2, 500);
        assertEquals(2, testProcessor.getPublishedStates().size(), "First press should publish 2 states");

        testComponent.setState(new InputState(InputStateCalculator.ValidInputState.CLOSED));
        captor.update(testComponent);

        testProcessor.waitForPublications(4, 1000);

        List<InputState> allPublished = testProcessor.getPublishedStates();
        assertEquals(4, allPublished.size(), "Should have 2 press cycles = 4 publications");

        assertEquals(InputStateCalculator.ValidInputState.SHORT_PRESS, allPublished.get(0).getState());
        assertEquals(InputStateCalculator.ValidInputState.NOT_PRESSED, allPublished.get(1).getState());
        assertEquals(InputStateCalculator.ValidInputState.LONG_PRESS, allPublished.get(2).getState());
        assertEquals(InputStateCalculator.ValidInputState.NOT_PRESSED, allPublished.get(3).getState());
    }

    @Test
    @Timeout(2)
    void testCancelledPress_NoPublicationIfNeverStarted() throws InterruptedException {
        testComponent.setState(new InputState(InputStateCalculator.ValidInputState.OPEN));

        captor.update(testComponent);
        Thread.sleep(200);

        assertTrue(testProcessor.getPublishedStates().isEmpty(),
                "Should not publish anything if button was never pressed");
    }

    @Test
    @Timeout(2)
    void testVeryShortPress_StillDetectedCorrectly() throws InterruptedException {
        testComponent.setState(new InputState(InputStateCalculator.ValidInputState.CLOSED));

        captor.update(testComponent);
        Thread.sleep(10);
        testComponent.setState(new InputState(InputStateCalculator.ValidInputState.OPEN));
        captor.update(testComponent);

        testProcessor.waitForPublications(2, 500);

        List<InputState> published = testProcessor.getPublishedStates();
        assertEquals(2, published.size());
        assertEquals(InputStateCalculator.ValidInputState.SHORT_PRESS, published.get(0).getState());

        Long pressDuration = ((LongPressInputCaptor.Captor) published.get(0)).getPressDurationMillis();
        assertTrue(pressDuration < 500, "Very short press should be < 500ms");
    }

    /**
     * Thread-safe test stub for MqttProcessor that captures published states
     */
    private static class TestMqttProcessor implements MqttPublisher {
        private final List<InputState> publishedStates = new CopyOnWriteArrayList<>();
        private final Object lock = new Object();

        @Override
        public void publishState(ComponentSpec component, State<?> state) {
            synchronized (lock) {
                publishedStates.add((InputState) state);
                lock.notifyAll(); // Wake up waiting threads
            }
        }

        public List<InputState> getPublishedStates() {
            synchronized (lock) {
                return new ArrayList<>(publishedStates);
            }
        }

        /**
         * Wait until the expected number of publications have been received
         * @param expectedCount Number of publications to wait for
         * @param timeoutMillis Maximum time to wait in milliseconds
         * @return true if expected count was reached, false if timeout
         */
        public boolean waitForPublications(int expectedCount, long timeoutMillis) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutMillis;
            synchronized (lock) {
                while (publishedStates.size() < expectedCount) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) {
                        return false; // Timeout
                    }
                    lock.wait(remaining);
                }
                return true;
            }
        }
    }
}
