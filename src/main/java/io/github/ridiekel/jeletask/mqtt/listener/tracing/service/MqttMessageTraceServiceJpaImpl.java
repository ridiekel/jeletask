package io.github.ridiekel.jeletask.mqtt.listener.tracing.service;

import io.github.ridiekel.jeletask.mqtt.listener.tracing.MessageDirection;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.MqttMessageTrace;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.repository.MqttMessageTraceRepository;
import io.github.ridiekel.jeletask.mqtt.listener.tracing.sse.mqttmessage.MqttMessageTraceSseController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "sba.server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MqttMessageTraceServiceJpaImpl implements MqttMessageTraceService {
    private final MqttMessageTraceRepository repository;
    private final MqttMessageTraceSseController mqttMessageTraceSseController;

    public MqttMessageTraceServiceJpaImpl(
            MqttMessageTraceRepository repository,
            MqttMessageTraceSseController mqttMessageTraceSseController
    ) {
        this.repository = repository;
        this.mqttMessageTraceSseController = mqttMessageTraceSseController;
    }

    @Transactional
    public void receive(String topic, String payload, Integer qos, boolean retained) {
        save(MessageDirection.RECEIVE, topic, payload, qos, retained);
    }

    @Transactional
    public void publish(String topic, String payload, Integer qos, boolean retained) {
        save(MessageDirection.PUBLISH, topic, payload, qos, retained);
    }

    private void save(MessageDirection direction, String topic, String payload, Integer qos, boolean retained) {
        MqttMessageTrace t = new MqttMessageTrace();
        t.setDirection(direction);
        t.setTopic(topic);
        t.setPayload(payload != null ? payload : "");
        t.setQos(qos);
        t.setRetained(retained);
        repository.save(t);
        mqttMessageTraceSseController.publish(t);
    }
}
