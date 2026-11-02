package io.github.ridiekel.jeletask;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

class TestProgressListenerTest {
    private static final String FIXTURE = "jeletask.progress-fixture";
    // Otherwise the listener from META-INF/services would also print the fixture into the CI log
    private static final LauncherConfig WITHOUT_REGISTERED_LISTENERS = LauncherConfig.builder()
            .enableTestExecutionListenerAutoRegistration(false)
            .build();

    @Test
    void everyLineShowsHowFarTheRunIsAndTheLastOneIsComplete() {
        List<String> lines = run(true);

        // plain, parameterized (counts once for its 3 cases) and the 2 tests of the skipped nested class
        assertThat(lines).allMatch(line -> line.startsWith("[test] "))
                .filteredOn(line -> line.contains("✔ ok"))
                .hasSize(4)
                .anyMatch(line -> line.contains("Fixture.plain") && line.contains("/4] "))
                .anyMatch(line -> line.contains("Fixture.parameterized [3]"));
        assertThat(lines.getLast()).contains("[100% 4/4] ").contains("finished");
    }

    @Test
    void outsideCiNothingIsPrinted() {
        assertThat(run(false)).isEmpty();
    }

    private static List<String> run(boolean enabled) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        LauncherDiscoveryRequest request = request()
                .selectors(selectClass(Fixture.class))
                .configurationParameter(FIXTURE, "true")
                .build();
        LauncherFactory.create(WITHOUT_REGISTERED_LISTENERS).execute(request, new TestProgressListener(enabled, new PrintStream(bytes, true, StandardCharsets.UTF_8)));
        return bytes.toString(StandardCharsets.UTF_8).lines()
                .map(line -> line.replaceAll("\u001B\\[[0-9;]*m", ""))
                .toList();
    }

    // Only runs when launched by the tests above, never as part of the suite itself
    static class OnlyWhenLaunchedByTheTest implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            return context.getConfigurationParameter(FIXTURE).isPresent()
                    ? ConditionEvaluationResult.enabled("launched by TestProgressListenerTest")
                    : ConditionEvaluationResult.disabled("fixture of TestProgressListenerTest");
        }
    }

    @ExtendWith(OnlyWhenLaunchedByTheTest.class)
    static class Fixture {
        @Test
        void plain() {
            assertThat(FIXTURE).isNotEmpty();
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3})
        void parameterized(int value) {
            assertThat(value).isPositive();
        }

        @Nested
        @Disabled("a skipped class must still count its tests as done")
        class Skipped {
            @Test
            void one() {
                assertThat(FIXTURE).isNotEmpty();
            }

            @Test
            void two() {
                assertThat(FIXTURE).isNotEmpty();
            }
        }
    }
}
