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
    @Query("delete from MqttMessageTrace m where m.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
