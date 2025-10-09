package io.github.ridiekel.jeletask.mqtt.listener.tracing;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MqttTraceController {

    private final MqttMessageTraceRepository repo;

    public MqttTraceController(MqttMessageTraceRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/api/traces")
    public List<MqttMessageTrace> all(
            @RequestParam(required = false) String topic
    ) {
        if (topic != null && !topic.isBlank()) {
            return repo.findByTopicOrderByCreatedAtDesc(topic);
        }
        return repo.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }
}
