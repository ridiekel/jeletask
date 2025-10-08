package io.github.ridiekel.jeletask.mqtt.container.mqtt;

public interface MatchTest {
    MatchTestResult test(MqttContainer.MqttCapture capture);

    record MatchTestResult(
            boolean result,
            String actual
    ) {
    }
}
