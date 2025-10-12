package io.github.ridiekel.jeletask.sba.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.MqttMessageTrace;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.repository.MqttMessageTraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Endpoint(id = "traces")
@RequiredArgsConstructor
public class MqttMessageTraceEndpoint {
    private final CentralUnit centralUnit;
    private final ObjectMapper objectMapper;
    private final MqttMessageTraceRepository repo;

    @ReadOperation
    public List<MqttMessageTrace> all() {
        return repo.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }
}
