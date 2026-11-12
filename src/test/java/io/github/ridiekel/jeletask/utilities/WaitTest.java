package io.github.ridiekel.jeletask.utilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaitTest {
    private static final Duration POLL = Duration.ofMillis(1);

    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    void aConditionThatHoldsReturnsWithoutPolling() {
        AtomicInteger checks = new AtomicInteger();

        Wait.until("ready", Duration.ofSeconds(5), Duration.ofSeconds(5), () -> checks.incrementAndGet() > 0);

        assertThat(checks).hasValue(1);
    }

    @Test
    void pollsUntilTheConditionHolds() {
        AtomicInteger checks = new AtomicInteger();

        Wait.until("ready", Duration.ofSeconds(5), POLL, () -> checks.incrementAndGet() == 3);

        assertThat(checks).hasValue(3);
    }

    @Test
    void givesUpOnceTheTimeIsUp() {
        Duration atMost = Duration.ofMillis(50);
        long start = System.nanoTime();

        assertThatThrownBy(() -> Wait.until("ready", atMost, POLL, () -> false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ready: not done within PT0.05S");

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(50)).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void theFirstCheckCanComeAfterADelay() {
        long start = System.nanoTime();
        AtomicInteger checks = new AtomicInteger();

        Wait.until("ready", Duration.ofSeconds(5), Duration.ofMillis(50), POLL, () -> checks.incrementAndGet() > 0);

        assertThat(checks).hasValue(1);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(50));
    }

    @Test
    void anInterruptStopsTheWaitAndIsKept() {
        Duration atMost = Duration.ofSeconds(5);
        Thread.currentThread().interrupt();

        assertThatThrownBy(() -> Wait.until("ready", atMost, POLL, () -> false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ready: interrupted")
                .hasCauseInstanceOf(InterruptedException.class);

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }
}
