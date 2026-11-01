package io.github.ridiekel.jeletask.client;

import org.awaitility.core.ConditionFactory;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.awaitility.Awaitility.await;

/**
 * The threads of the clients a test started.
 * <p>
 * Awaitility makes itself the default uncaught exception handler of the JVM while it waits. An io thread dying after its
 * test ended would fail whatever the next test waits for, also inside the client, so a test has to outlive its client.
 */
public final class ClientThreads {
    private static final Set<String> NAMES = Set.of("io", "keep-alive", "event-listener");

    private final Set<Thread> before = Thread.getAllStackTraces().keySet();

    /**
     * Waits without taking over the exceptions of other threads: an io thread may die in a test, that is what they are about.
     */
    public static ConditionFactory eventually() {
        return await().dontCatchUncaughtExceptions().atMost(Duration.ofSeconds(20));
    }

    public List<Thread> started() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> !this.before.contains(thread))
                .filter(thread -> NAMES.contains(thread.getName()))
                .toList();
    }

    public void awaitAllGone() {
        eventually().until(() -> started().isEmpty());
    }
}
