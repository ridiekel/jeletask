package io.github.ridiekel.jeletask.mqtt.listener.tracing.sse.centralunit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class CentralUnitSseHeartbeat {

    private final CentralUnitSsePublisher publisher;

    CentralUnitSseHeartbeat(CentralUnitSsePublisher publisher) {
        this.publisher = publisher;
    }

    @Scheduled(fixedRate = 5000)
    public void beat() {
        publisher.heartbeat();
    }
}
