package io.github.ridiekel.jeletask.mqtt.listener.tracing.job;

import io.github.ridiekel.jeletask.mqtt.listener.tracing.config.MqttMessageTraceProperties;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.repository.MqttMessageTraceRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@ConditionalOnProperty(prefix = "mqtt.trace.cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "sba.server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MqttMessageTraceCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageTraceCleanupJob.class);

    private final MqttMessageTraceRepository repository;
    private final MqttMessageTraceProperties properties;
    private final EntityManager entityManager;

    public MqttMessageTraceCleanupJob(MqttMessageTraceRepository repository, MqttMessageTraceProperties properties, EntityManager entityManager) {
        this.repository = repository;
        this.properties = properties;
        this.entityManager = entityManager;
    }

    @Scheduled(fixedDelayString = "#{@mqttTraceProperties.cleanup.interval.toMillis()}")
    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now().minus(properties.getRetention());
        int batchSize = 100;
        int totalDeleted = 0;
        int deleted;

        do {
            deleted = repository.deleteOlderThanBatch(cutoff, batchSize);
            totalDeleted += deleted;
            entityManager.flush();
            entityManager.clear();
        } while (deleted > 0);

        if (totalDeleted > 0) {
            log.info("MQTT trace cleanup: {} records verwijderd ouder dan {}", totalDeleted, cutoff);
        } else {
            log.debug("MQTT trace cleanup: geen records ouder dan {}", cutoff);
        }
    }
}
