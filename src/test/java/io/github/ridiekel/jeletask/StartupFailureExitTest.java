package io.github.ridiekel.jeletask;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplicationAotProcessor;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A startup that fails has to end the process: only then the restart policy of the container brings the bridge back.
 * Threads of the application that are still running must not keep a JVM alive that never became a bridge.
 * Needs a JVM of its own, the exit is the point: once in the JVM run is enough.
 * The ahead-of-time processing of the native build is no startup, and must not end that way.
 */
@DisabledInNativeImage
class StartupFailureExitTest {
    private static final String LOG_FILE = "application.log";

    @TempDir
    private Path workDir;

    @Test
    void theProcessExitsWhenTheStartupFails() throws IOException, InterruptedException {
        Process process = startApplication("not-a-port");
        try {
            boolean exited = process.waitFor(60, TimeUnit.SECONDS);

            assertThat(exited).as("the process ended by itself, its log:%n%s", log()).isTrue();
            assertThat(process.exitValue()).isEqualTo(1);
            assertThat(log()).contains("TELETASK_MQTT_PORT is not a port number");
        } finally {
            process.destroyForcibly();
        }
    }

    /**
     * A broker that is not there yet is no failed startup: the bridge waits for it, and can still be stopped.
     */
    @Test
    void theProcessKeepsTryingWhenTheBrokerCannotBeReachedAndStopsWhenAsked() throws IOException, InterruptedException {
        Process process = startApplication(String.valueOf(closedPort()));
        try {
            // The second attempt comes 10 seconds after the first
            Awaitility.await("a second attempt").atMost(Duration.ofSeconds(60)).until(() -> log().contains("(attempt 2)"));
            assertThat(process.isAlive()).as("the process keeps running, its log:%n%s", log()).isTrue();
            assertThat(log()).contains("started!");

            process.destroy();

            assertThat(process.waitFor(10, TimeUnit.SECONDS)).as("the process stopped when asked, its log:%n%s", log()).isTrue();
        } finally {
            process.destroyForcibly();
        }
    }

    /**
     * The native build runs main to collect the beans, and Spring ends that run with an exception once it has them.
     * Taken for a failed startup, the exit fails the native build. Same entry point as {@code spring-boot:process-aot}.
     */
    @Test
    void theAheadOfTimeProcessingIsNoFailedStartup() throws IOException, InterruptedException {
        Path output = this.workDir.resolve("aot");
        Process process = startJava(String.valueOf(closedPort()),
                SpringApplicationAotProcessor.class.getName(),
                Teletask2MqttApplication.class.getName(),
                output.resolve("sources").toString(),
                output.resolve("resources").toString(),
                output.resolve("classes").toString(),
                "io.github.ridiekel",
                "jeletask");
        try {
            boolean exited = process.waitFor(60, TimeUnit.SECONDS);

            assertThat(exited).as("the processing ended, its log:%n%s", log()).isTrue();
            assertThat(process.exitValue()).as("the exit value, the log:%n%s", log()).isZero();
            assertThat(output.resolve("sources")).isNotEmptyDirectory();
        } finally {
            process.destroyForcibly();
        }
    }

    private Process startApplication(String mqttPort) throws IOException {
        return startJava(mqttPort, Teletask2MqttApplication.class.getName());
    }

    private Process startJava(String mqttPort, String... mainClassAndArguments) throws IOException {
        List<String> command = new ArrayList<>(List.of(ProcessHandle.current().info().command().orElse("java"), "-cp", applicationClasspath()));
        command.addAll(List.of(mainClassAndArguments));
        ProcessBuilder builder = new ProcessBuilder(command).directory(this.workDir.toFile()).redirectErrorStream(true).redirectOutput(this.workDir.resolve(LOG_FILE).toFile());
        Map<String, String> environment = builder.environment();
        environment.put("TELETASK_CONFIGFILE", Path.of("src", "test", "resources", "test-config.json").toUri().toString());
        environment.put("TELETASK_CENTRAL_HOST", "127.0.0.1");
        environment.put("TELETASK_CENTRAL_PORT", String.valueOf(closedPort()));
        environment.put("TELETASK_MQTT_HOST", "127.0.0.1");
        environment.put("TELETASK_MQTT_PORT", mqttPort);
        // The test classpath has a web server on it, the application has none
        environment.put("SPRING_MAIN_WEB_APPLICATION_TYPE", "none");
        return builder.start();
    }

    private String log() throws IOException {
        return Files.readString(this.workDir.resolve(LOG_FILE));
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
