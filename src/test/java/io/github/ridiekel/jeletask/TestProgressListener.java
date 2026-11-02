package io.github.ridiekel.jeletask;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prints a line when a test starts and when it ends, so a CI log shows which test runs and which one hangs.
 * <p>
 * Only on CI: locally the IDE and Maven already show this. Registered through {@code META-INF/services}, which also
 * works in the native test image. The lines start with {@code [test]}, the pipeline filters its log on that.
 * <p>
 * Every finished or skipped line shows how far the run is. It counts test methods known up front: a parameterized
 * or repeated test only gets its cases while running, so it counts as one and is done when all its cases are.
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
    private final Set<String> remaining = ConcurrentHashMap.newKeySet();
    private TestPlan plan;
    private int total;

    public TestProgressListener() {
        this(System.getenv("CI") != null, System.out);
    }

    TestProgressListener(boolean enabled, PrintStream out) {
        this.enabled = enabled;
        this.out = out;
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        this.plan = testPlan;
        this.remaining.clear();
        testPlan.getRoots().forEach(root -> testPlan.getDescendants(root).stream()
                .filter(TestProgressListener::isTestMethod)
                .forEach(test -> this.remaining.add(test.getUniqueId())));
        this.total = this.remaining.size();
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (this.enabled) {
            print(progress() + BOLD + "finished" + RESET);
        }
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
        markDone(test);
        if (!this.enabled || !test.isTest()) {
            return;
        }
        print(progress() + YELLOW + "↷ skip  " + RESET + name(test) + YELLOW + " (" + reason + ")" + RESET);
    }

    @Override
    public void executionFinished(TestIdentifier test, TestExecutionResult result) {
        markDone(test);
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
        print(progress() + status + name(test) + duration);
    }

    // A skipped class, or one whose @BeforeAll failed, never reports its tests: they are done along with it
    private void markDone(TestIdentifier test) {
        this.remaining.remove(test.getUniqueId());
        if (this.plan != null && test.isContainer()) {
            this.plan.getDescendants(test).forEach(descendant -> this.remaining.remove(descendant.getUniqueId()));
        }
    }

    private String progress() {
        if (this.total == 0) {
            return "";
        }
        int done = this.total - this.remaining.size();
        String counter = String.valueOf(this.total);
        return String.format("[%3d%% %" + counter.length() + "d/%s] ", done * 100 / this.total, done, counter);
    }

    private static boolean isTestMethod(TestIdentifier test) {
        return test.getSource().orElse(null) instanceof MethodSource;
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
