package io.github.ridiekel.jeletask.mqtt.listener.tracing.sse;

import io.github.ridiekel.jeletask.mqtt.listener.tracing.MqttMessageTrace;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
public class MqttMessageTraceSseController {
    private final List<SseEmitter> emitters = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final MqttMessageTraceSseBuffer repo;

    public MqttMessageTraceSseController(MqttMessageTraceSseBuffer repo) {
        this.repo = repo;
    }

    @GetMapping(value = "/actuator/traces/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));

        // Stuur direct iets zodat de client/proxy de stream “opent”
        try {
            emitter.send(SseEmitter.event().name("hello").data(java.time.Instant.now().toString()));
            // Korte backlog om UI meteen te vullen / connectie te bevestigen
            var backlog = repo.findAllNewestFirst().stream().limit(10).toList();
            for (var ev : backlog) {
                emitter.send(SseEmitter.event().name("mqtt").data(ev));
            }
        } catch (Exception e) {
            emitter.completeWithError(e);
            emitters.remove(emitter);
        }

        // Headers om buffering te voorkomen
        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no") // voor Nginx
                .body(emitter);
    }

    public void publish(MqttMessageTrace event) {
        for (SseEmitter e : emitters) {
            try {
                e.send(SseEmitter.event().name("mqtt").data(event));
            } catch (Exception ex) {
                e.complete();
                emitters.remove(e);
            }
        }
    }

    public void keepAlive() {
        for (SseEmitter e : emitters) {
            try {
                e.send(SseEmitter.event().comment("keepalive"));
            } catch (Exception ex) {
                e.complete();
                emitters.remove(e);
            }
        }
    }
}
