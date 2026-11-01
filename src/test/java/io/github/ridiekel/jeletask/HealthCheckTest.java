package io.github.ridiekel.jeletask;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

class HealthCheckTest {
    private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");

    @TempDir
    Path dir;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    @Test
    void healthyWhenBothConnected() throws Exception {
        healthCheck(NOW).write(true, true);

        Assertions.assertThat(check(NOW)).isZero();
        Assertions.assertThat(output.toString()).startsWith("HEALTHY: teletask=UP, mqtt=UP");
    }

    @Test
    void unhealthyWhenTeletaskDisconnected() throws Exception {
        healthCheck(NOW).write(false, true);

        Assertions.assertThat(check(NOW)).isOne();
        Assertions.assertThat(output.toString()).startsWith("UNHEALTHY: teletask=DOWN, mqtt=UP");
    }

    @Test
    void unhealthyWhenMqttDisconnected() throws Exception {
        healthCheck(NOW).write(true, false);

        Assertions.assertThat(check(NOW)).isOne();
        Assertions.assertThat(output.toString()).startsWith("UNHEALTHY: teletask=UP, mqtt=DOWN");
    }

    @Test
    void unhealthyWhenStatusIsStale() throws Exception {
        healthCheck(NOW).write(true, true);

        Assertions.assertThat(check(NOW.plus(HealthCheck.MAX_AGE))).isZero();
        Assertions.assertThat(check(NOW.plus(HealthCheck.MAX_AGE).plusSeconds(1))).isOne();
        Assertions.assertThat(output.toString()).contains("UNHEALTHY: status not updated");
    }

    @Test
    void unhealthyWithoutStatusFile() {
        Assertions.assertThat(check(NOW)).isOne();
        Assertions.assertThat(output.toString()).startsWith("UNHEALTHY: no status written yet");
    }

    @Test
    void unhealthyWithInvalidStatusFile() throws Exception {
        Files.writeString(file(), "garbage");

        Assertions.assertThat(check(NOW)).isOne();
        Assertions.assertThat(output.toString()).startsWith("UNHEALTHY: invalid status file");
    }

    @Test
    void unhealthyWithoutTimestamp() throws Exception {
        Files.writeString(file(), "teletask=UP\nmqtt=UP\n");

        Assertions.assertThat(check(NOW)).isOne();
        Assertions.assertThat(output.toString()).startsWith("UNHEALTHY: invalid status file");
    }

    @ParameterizedTest
    @ValueSource(strings = {"mqtt=UP", "teletask=UP", "teletask=up\nmqtt=up", "teletask=UP\nmqtt=DOWN", ""})
    void unhealthyUnlessBothAreExactlyUp(String status) throws Exception {
        Files.writeString(file(), "updated=" + NOW + "\n" + status + "\n");

        Assertions.assertThat(check(NOW)).isOne();
        Assertions.assertThat(output.toString()).startsWith("UNHEALTHY: teletask=");
    }

    @Test
    void unhealthyWhenTheStatusFileCannotBeRead() throws Exception {
        Files.createDirectory(file());

        Assertions.assertThat(check(NOW)).isOne();
        Assertions.assertThat(output.toString()).startsWith("UNHEALTHY: could not read");
    }

    @Test
    void unhealthyWhenTheStatusFileIsNotText() throws Exception {
        Files.write(file(), new byte[]{(byte) 0xC3, 0x28});

        Assertions.assertThat(check(NOW)).isOne();
        Assertions.assertThat(output.toString()).startsWith("UNHEALTHY: could not read");
    }

    @Test
    void aClockThatWentBackDoesNotMakeTheStatusStale() throws Exception {
        healthCheck(NOW).write(true, true);

        Assertions.assertThat(check(NOW.minusSeconds(3600))).isZero();
    }

    @Test
    void writingReplacesThePreviousStatusAndLeavesNothingBehind() throws Exception {
        healthCheck(NOW).write(false, false);
        healthCheck(NOW).write(true, true);

        Assertions.assertThat(check(NOW)).isZero();
        try (Stream<Path> files = Files.list(dir)) {
            Assertions.assertThat(files).containsExactly(file());
        }
    }

    @Test
    void writingToAMissingDirectoryFails() {
        HealthCheck healthCheck = new HealthCheck(dir.resolve("missing").resolve("health"), Clock.fixed(NOW, ZoneOffset.UTC));

        Assertions.assertThatThrownBy(() -> healthCheck.write(true, true)).isInstanceOf(IOException.class);
    }

    @Test
    @DisabledInNativeImage
    void mainRunsFromThisClassAloneAndExitsWithOneWithoutStatus() throws Exception {
        Assertions.assertThat(runMain()).isOne();
    }

    @Test
    @DisabledInNativeImage
    void mainExitsWithZeroWhenHealthy() throws Exception {
        new HealthCheck(dir.resolve("jeletask2mqtt.health"), Clock.systemUTC()).write(true, true);

        Assertions.assertThat(runMain()).isZero();
    }

    /**
     * The JVM image runs {@link HealthCheck#main} with nothing but the application classes on its classpath:
     * this test breaks as soon as the class needs a dependency.
     */
    private int runMain() throws Exception {
        String classes = Path.of(HealthCheck.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Djava.io.tmpdir=" + dir,
                "-cp", classes,
                HealthCheck.class.getName()
        ).redirectErrorStream(true).redirectOutput(dir.resolve("main.log").toFile()).start();

        try {
            Assertions.assertThat(process.waitFor(30, TimeUnit.SECONDS)).as("exited in time").isTrue();
        } finally {
            process.destroyForcibly();
        }
        Assertions.assertThat(Files.readString(dir.resolve("main.log"))).containsAnyOf("HEALTHY: ", "UNHEALTHY: ");
        return process.exitValue();
    }

    private int check(Instant now) {
        return healthCheck(now).check(new PrintStream(output, true));
    }

    private HealthCheck healthCheck(Instant now) {
        return new HealthCheck(file(), Clock.fixed(now, ZoneOffset.UTC));
    }

    private Path file() {
        return dir.resolve("health");
    }
}
