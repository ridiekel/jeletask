package io.github.ridiekel.jeletask.mqtt.listener.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@ConditionalOnProperty(prefix = "mqtt.trace.cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MqttTraceCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(MqttTraceCleanupJob.class);

    private final MqttMessageTraceRepository repository;
    private final MqttTraceProperties properties;

    public MqttTraceCleanupJob(MqttMessageTraceRepository repository, MqttTraceProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "#{@mqttTraceProperties.cleanup.interval.toMillis()}")
    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now().minus(properties.getRetention());
        Long deleted = repository.deleteByCreatedAtBefore(cutoff);
        if (deleted != null && deleted > 0) {
            log.info("MQTT trace cleanup: {} records removed older than {}", deleted, cutoff);
        } else {
            log.debug("MQTT trace cleanup: no records older than {}", cutoff);
        }
    }
}
