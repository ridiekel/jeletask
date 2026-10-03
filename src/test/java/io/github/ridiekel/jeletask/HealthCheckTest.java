package io.github.ridiekel.jeletask;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

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
