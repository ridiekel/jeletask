package io.github.ridiekel.jeletask.utilities;

import java.time.Duration;
import java.util.function.BooleanSupplier;

/**
 * Polls a condition in the calling thread until it holds or the time is up.
 */
public final class Wait {
    private Wait() {
    }

    /**
     * Checks right away, then after every {@code pollInterval}.
     *
     * @throws IllegalStateException when the condition does not hold within {@code atMost}, or the wait is interrupted
     */
    public static void until(String description, Duration atMost, Duration pollInterval, BooleanSupplier condition) {
        until(description, atMost, Duration.ZERO, pollInterval, condition);
    }

    /**
     * Checks for the first time after {@code firstCheckAfter}, then after every {@code pollInterval}.
     *
     * @throws IllegalStateException when the condition does not hold within {@code atMost}, or the wait is interrupted
     */
    public static void until(String description, Duration atMost, Duration firstCheckAfter, Duration pollInterval, BooleanSupplier condition) {
        long start = System.nanoTime();
        pause(description, firstCheckAfter);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() - start >= atMost.toNanos()) {
                throw new IllegalStateException(String.format("%s: not done within %s", description, atMost));
            }
            pause(description, pollInterval);
        }
    }

    private static void pause(String description, Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(description + ": interrupted", e);
        }
    }
}
