package io.github.ridiekel.jeletask.mqtt.listener.tracing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(
    name = "mqtt_message_trace",
    indexes = {
        @Index(name = "idx_mqtt_trace_topic_created", columnList = "topic, created_at"),
        @Index(name = "idx_mqtt_trace_direction_created", columnList = "direction, created_at"),
        @Index(name = "idx_mqtt_trace_created", columnList = "created_at")
    }
)
public class MqttMessageTrace {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 512)
    private String topic;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MessageDirection direction;

    @Column
    private Integer qos;

    @Column(nullable = false)
    private boolean retained = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
