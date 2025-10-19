package io.github.ridiekel.jeletask.sba.endpoint;

import io.github.ridiekel.jeletask.mqtt.listener.tracing.MqttMessageTrace;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.repository.MqttMessageTraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Endpoint(id = "traces")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sba.server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MqttMessageTraceEndpoint {
    private final MqttMessageTraceRepository repo;

    @ReadOperation
    public List<MqttMessageTrace> all() {
        Pageable p = PageRequest.of(0, 1000, Sort.by("createdAt").descending());
        return repo.findAll(p).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }
}
