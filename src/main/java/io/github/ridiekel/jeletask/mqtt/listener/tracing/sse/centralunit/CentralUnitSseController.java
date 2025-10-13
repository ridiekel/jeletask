package io.github.ridiekel.jeletask.mqtt.listener.tracing.sse.centralunit;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class CentralUnitSseController {

    private final CentralUnitSsePublisher publisher;

    public CentralUnitSseController(CentralUnitSsePublisher publisher) {
        this.publisher = publisher;
    }

    @GetMapping(path = "/actuator/centralunit/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return publisher.subscribe();
    }
}
