package io.github.ridiekel.jeletask.mqtt.listener;

import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.DockerClientFactory;

import java.io.IOException;

/**
 * The resilience scenarios against Toxiproxy and mosquitto in containers. Skipped without Docker.
 */
@EnabledIf("dockerAvailable")
class ResilienceToxiproxyTest extends ResilienceScenarios {
    static boolean dockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    @Override
    ResilienceNetwork startNetwork(int centralPort) throws IOException {
        return ResilienceContainers.start(centralPort);
    }
}
