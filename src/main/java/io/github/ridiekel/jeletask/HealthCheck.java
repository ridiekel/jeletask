package io.github.ridiekel.jeletask;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Properties;

/**
 * The running application periodically writes its connection status to a file. Running the
 * application with {@code --healthcheck} reads that file without starting Spring and exits with
 * 0 when healthy, 1 otherwise. This works in images without a shell or HTTP client (native).
 * The JVM image runs {@link #main} instead, from this class alone, so a check does not pay for
 * opening the Spring Boot jar: keep this class free of dependencies outside the JDK.
 */
public final class HealthCheck {
    public static final String ARGUMENT = "--healthcheck";

    static final Duration MAX_AGE = Duration.ofSeconds(30);

    private static final HealthCheck DEFAULT = new HealthCheck(Path.of(System.getProperty("java.io.tmpdir"), "jeletask2mqtt.health"), Clock.systemUTC());

    private final Path file;
    private final Clock clock;

    HealthCheck(Path file, Clock clock) {
        this.file = file;
        this.clock = clock;
    }

    public static HealthCheck getDefault() {
        return DEFAULT;
    }

    static void main(String[] args) {
        System.exit(DEFAULT.check(System.out));
    }

    public void write(boolean teletaskConnected, boolean mqttConnected) throws IOException {
        String content = "updated=" + clock.instant() + "\n"
                + "teletask=" + upOrDown(teletaskConnected) + "\n"
                + "mqtt=" + upOrDown(mqttConnected) + "\n";
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, content);
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public int check(PrintStream out) {
        Properties status = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            status.load(reader);
        } catch (NoSuchFileException e) {
            out.println("UNHEALTHY: no status written yet (" + file + ")");
            return 1;
        } catch (IOException e) {
            out.println("UNHEALTHY: could not read " + file + ": " + e.getMessage());
            return 1;
        }

        String summary = "teletask=" + status.getProperty("teletask") + ", mqtt=" + status.getProperty("mqtt") + ", updated=" + status.getProperty("updated");

        Instant updated;
        try {
            updated = Instant.parse(status.getProperty("updated", ""));
        } catch (DateTimeParseException e) {
            out.println("UNHEALTHY: invalid status file (" + summary + ")");
            return 1;
        }
        if (updated.isBefore(clock.instant().minus(MAX_AGE))) {
            out.println("UNHEALTHY: status not updated for more than " + MAX_AGE.toSeconds() + "s (" + summary + ")");
            return 1;
        }
        if (!"UP".equals(status.getProperty("teletask")) || !"UP".equals(status.getProperty("mqtt"))) {
            out.println("UNHEALTHY: " + summary);
            return 1;
        }
        out.println("HEALTHY: " + summary);
        return 0;
    }

    private static String upOrDown(boolean up) {
        return up ? "UP" : "DOWN";
    }
}
