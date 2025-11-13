package io.github.ridiekel.jeletask.mqtt.listener.tracing.repository;

import io.github.ridiekel.jeletask.mqtt.listener.tracing.MqttMessageTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface MqttMessageTraceRepository extends JpaRepository<MqttMessageTrace, UUID> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "delete from mqtt_message_trace where id in " +
            "(select id from mqtt_message_trace where created_at < :cutoff limit :batchSize)",
            nativeQuery = true)
    int deleteOlderThanBatch(@Param("cutoff") Instant cutoff, @Param("batchSize") int batchSize);
}
