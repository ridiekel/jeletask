package io.github.ridiekel.jeletask.mqtt.listener.tracing.repository;

import io.github.ridiekel.jeletask.Teletask2MqttApplication;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.MessageDirection;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.MqttMessageTrace;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = Teletask2MqttApplication.class)
class MqttMessageTraceRepositoryTest {

    @Autowired
    private MqttMessageTraceRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @AfterEach
    void cleanup() {
        repository.deleteAll();
        entityManager.flush();
    }

    @Test
    void deleteOlderThanBatchRemovesRecordsInMultipleBatches() {
        int oldRecordCount = 5;
        int recentRecordCount = 3;
        int batchSize = 2;

        List<UUID> oldIds = new ArrayList<>();
        List<UUID> recentIds = new ArrayList<>();

        for (int i = 0; i < oldRecordCount; i++) {
            MqttMessageTrace old = repository.save(buildTrace("old/topic/" + i));
            oldIds.add(old.getId());
        }

        for (int i = 0; i < recentRecordCount; i++) {
            MqttMessageTrace recent = repository.save(buildTrace("recent/topic/" + i));
            recentIds.add(recent.getId());
        }

        entityManager.flush();

        Instant cutoff = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        for (UUID oldId : oldIds) {
            entityManager.createNativeQuery("""
                    update mqtt_message_trace
                       set created_at = :createdAt
                     where id = :id
                    """)
                    .setParameter("createdAt", cutoff.minusSeconds(60))
                    .setParameter("id", oldId)
                    .executeUpdate();
        }

        entityManager.flush();
        entityManager.clear();

        int totalDeleted = 0;
        int deleted;
        int iterations = 0;

        do {
            deleted = repository.deleteOlderThanBatch(cutoff, batchSize);
            totalDeleted += deleted;
            entityManager.flush();
            entityManager.clear();
            iterations++;
        } while (deleted > 0);

        assertThat(totalDeleted)
                .as("All old records should be deleted")
                .isEqualTo(oldRecordCount);

        assertThat(iterations)
                .as("With batchSize %d for %d records we expect at least %d iterations",
                        batchSize, oldRecordCount, (oldRecordCount + batchSize - 1) / batchSize)
                .isGreaterThanOrEqualTo((oldRecordCount + batchSize - 1) / batchSize);

        for (UUID oldId : oldIds) {
            assertThat(repository.existsById(oldId))
                    .as("Old record %s should be deleted", oldId)
                    .isFalse();
        }

        for (UUID recentId : recentIds) {
            assertThat(repository.existsById(recentId))
                    .as("Recent record %s should be retained", recentId)
                    .isTrue();
        }

        assertThat(repository.count())
                .as("Only recent records should remain")
                .isEqualTo(recentRecordCount);
    }

    @Test
    void deleteOlderThanBatchReturnsZeroWhenNoOldRecords() {
        MqttMessageTrace recent = repository.save(buildTrace("recent/topic"));
        entityManager.flush();

        Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);

        int deleted = repository.deleteOlderThanBatch(cutoff, 10);

        assertThat(deleted).isZero();
        assertThat(repository.existsById(recent.getId())).isTrue();
    }

    @Test
    void deleteOlderThanBatchRespectsExactCutoffBoundary() {
        MqttMessageTrace beforeCutoff = repository.save(buildTrace("before"));
        MqttMessageTrace atCutoff = repository.save(buildTrace("at"));
        MqttMessageTrace afterCutoff = repository.save(buildTrace("after"));
        entityManager.flush();

        Instant cutoff = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        entityManager.createNativeQuery("""
                update mqtt_message_trace
                   set created_at = :createdAt
                 where id = :id
                """)
                .setParameter("createdAt", cutoff.minusSeconds(1))
                .setParameter("id", beforeCutoff.getId())
                .executeUpdate();

        entityManager.createNativeQuery("""
                update mqtt_message_trace
                   set created_at = :createdAt
                 where id = :id
                """)
                .setParameter("createdAt", cutoff)
                .setParameter("id", atCutoff.getId())
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        int deleted = repository.deleteOlderThanBatch(cutoff, 10);

        assertThat(deleted).isEqualTo(1);
        assertThat(repository.existsById(beforeCutoff.getId())).isFalse();
        assertThat(repository.existsById(atCutoff.getId())).isTrue();
        assertThat(repository.existsById(afterCutoff.getId())).isTrue();
    }

    private MqttMessageTrace buildTrace(String topic) {
        MqttMessageTrace trace = new MqttMessageTrace();
        trace.setTopic(topic);
        trace.setPayload("payload-" + UUID.randomUUID());
        trace.setDirection(MessageDirection.PUBLISH);
        trace.setQos(0);
        trace.setRetained(false);
        return trace;
    }
}
