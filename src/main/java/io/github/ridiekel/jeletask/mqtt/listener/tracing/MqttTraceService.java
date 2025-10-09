package io.github.ridiekel.jeletask.mqtt.listener.tracing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MqttTraceService {
    private final MqttMessageTraceRepository repository;

    public MqttTraceService(MqttMessageTraceRepository repository) {
        this.repository = repository;
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
    }
}
