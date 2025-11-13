package io.github.ridiekel.jeletask.mqtt.listener.tracing.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import io.github.ridiekel.jeletask.Teletask2MqttApplication;
import io.github.ridiekel.jeletask.Teletask2MqttTestApplication;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.MessageDirection;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.MqttMessageTrace;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ContextConfiguration(classes = Teletask2MqttApplication.class)
class MqttMessageTraceRepositoryTest {

    @Autowired
    private MqttMessageTraceRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @Commit
    void deleteOlderThanRemovesOnlyRecordsBeforeCutoff() {
        // arrange
        MqttMessageTrace recent = repository.save(buildTrace("recent/topic"));
        MqttMessageTrace old = repository.save(buildTrace("old/topic"));
        MqttMessageTrace boundary = repository.save(buildTrace("boundary/topic"));
        entityManager.flush();

        Instant cutoff = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // maak één record ouder dan de cutoff, en één exact op de grens
        entityManager.createNativeQuery("""
                update mqtt_message_trace
                   set created_at = :createdAt
                 where id = :id
                """)
            .setParameter("createdAt", cutoff.minusSeconds(10))
            .setParameter("id", old.getId())
            .executeUpdate();

        entityManager.createNativeQuery("""
                update mqtt_message_trace
                   set created_at = :createdAt
                 where id = :id
                """)
            .setParameter("createdAt", cutoff)
            .setParameter("id", boundary.getId())
            .executeUpdate();

        entityManager.clear();

        // act
        int deleted = repository.deleteOlderThan(cutoff);

        // assert
        assertThat(deleted).isEqualTo(1);
        assertThat(repository.existsById(old.getId())).isFalse();
        assertThat(repository.existsById(recent.getId())).isTrue();
        assertThat(repository.existsById(boundary.getId())).isTrue();
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
