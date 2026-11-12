package io.github.ridiekel.jeletask;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A startup that fails has to end the process: only then the restart policy of the container brings the bridge back.
 * Threads of the application that are still running must not keep a JVM alive that never became a bridge.
 * Needs a JVM of its own, the exit is the point: once in the JVM run is enough.
 */
@DisabledInNativeImage
class StartupFailureExitTest {
    @TempDir
    private Path workDir;

    @Test
    void theProcessExitsWhenTheBrokerCannotBeReachedAtStartup() throws IOException, InterruptedException {
        Path log = this.workDir.resolve("application.log");
        ProcessBuilder builder = new ProcessBuilder(
                ProcessHandle.current().info().command().orElse("java"),
                "-cp", applicationClasspath(),
                Teletask2MqttApplication.class.getName()
        ).directory(this.workDir.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        Map<String, String> environment = builder.environment();
        environment.put("TELETASK_CONFIGFILE", Path.of("src", "test", "resources", "test-config.json").toUri().toString());
        environment.put("TELETASK_CENTRAL_HOST", "127.0.0.1");
        environment.put("TELETASK_CENTRAL_PORT", String.valueOf(closedPort()));
        environment.put("TELETASK_MQTT_HOST", "127.0.0.1");
        environment.put("TELETASK_MQTT_PORT", String.valueOf(closedPort()));
        // The test classpath has a web server on it, the application has none
        environment.put("SPRING_MAIN_WEB_APPLICATION_TYPE", "none");

        Process process = builder.start();
        try {
            boolean exited = process.waitFor(60, TimeUnit.SECONDS);

            assertThat(exited).as("the process ended by itself, its log:%n%s", Files.readString(log)).isTrue();
            assertThat(process.exitValue()).isEqualTo(1);
            assertThat(Files.readString(log)).contains("Could not connect");
        } finally {
            process.destroyForcibly();
        }
    }

    /**
     * Without the test classes: they hold Spring beans of their own, which the application would pick up.
     */
    private static String applicationClasspath() {
        // Surefire starts the tests from a jar that only refers to the classpath, and keeps the real one here
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        return Arrays.stream(classpath.split(File.pathSeparator))
                .filter(entry -> !entry.endsWith("test-classes"))
                .collect(Collectors.joining(File.pathSeparator));
    }

    private static int closedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }
}
