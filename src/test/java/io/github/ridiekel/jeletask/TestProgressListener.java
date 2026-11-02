package io.github.ridiekel.jeletask;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prints a line when a test starts and when it ends, so a CI log shows which test runs and which one hangs.
 * <p>
 * Only on CI: locally the IDE and Maven already show this. Registered through {@code META-INF/services}, which also
 * works in the native test image. The lines start with {@code [test]}, the pipeline filters its log on that.
 */
public class TestProgressListener implements TestExecutionListener {
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";

    private final boolean enabled;
    private final PrintStream out;
    private final Map<String, Long> started = new ConcurrentHashMap<>();

    public TestProgressListener() {
        this(System.getenv("CI") != null, System.out);
    }

    TestProgressListener(boolean enabled, PrintStream out) {
        this.enabled = enabled;
        this.out = out;
    }

    @Override
    public void executionStarted(TestIdentifier test) {
        if (!this.enabled || !test.isTest()) {
            return;
        }
        this.started.put(test.getUniqueId(), System.nanoTime());
        print(CYAN + "▶ start " + RESET + name(test));
    }

    @Override
    public void executionSkipped(TestIdentifier test, String reason) {
        if (!this.enabled || !test.isTest()) {
            return;
        }
        print(YELLOW + "↷ skip  " + RESET + name(test) + YELLOW + " (" + reason + ")" + RESET);
    }

    @Override
    public void executionFinished(TestIdentifier test, TestExecutionResult result) {
        if (!this.enabled || !test.isTest()) {
            return;
        }
        Long start = this.started.remove(test.getUniqueId());
        String duration = start == null ? "" : " (" + (System.nanoTime() - start) / 1_000_000 + " ms)";
        String status = switch (result.getStatus()) {
            case SUCCESSFUL -> GREEN + "✔ ok    " + RESET;
            case ABORTED -> YELLOW + "↷ abort " + RESET;
            case FAILED -> RED + BOLD + "✘ FAIL  " + RESET;
        };
        print(status + name(test) + duration);
    }

    private void print(String line) {
        this.out.println(BOLD + "[test] " + RESET + line);
        this.out.flush();
    }

    static String name(TestIdentifier test) {
        if (test.getSource().orElse(null) instanceof MethodSource method) {
            String className = method.getClassName();
            String name = className.substring(className.lastIndexOf('.') + 1) + "." + method.getMethodName();
            // Parameterized and repeated tests share a method: their display name tells them apart
            return test.getDisplayName().startsWith("[") ? name + " " + test.getDisplayName() : name;
        }
        return test.getDisplayName();
    }
}
