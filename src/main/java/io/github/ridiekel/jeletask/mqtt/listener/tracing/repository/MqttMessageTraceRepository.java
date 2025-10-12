package io.github.ridiekel.jeletask.mqtt.listener.tracing.repository;

import io.github.ridiekel.jeletask.mqtt.listener.tracing.MessageDirection;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.MqttMessageTrace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MqttMessageTraceRepository extends JpaRepository<MqttMessageTrace, UUID> {
    List<MqttMessageTrace> findByTopicOrderByCreatedAtDesc(String topic);
    Long deleteByCreatedAtBefore(Instant cutoff);
    List<MqttMessageTrace> findByDirectionOrderByCreatedAtDesc(MessageDirection direction);
    List<MqttMessageTrace> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);
}
