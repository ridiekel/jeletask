package io.github.ridiekel.jeletask.mqtt.listener.tracing.service;

public interface MqttMessageTraceService {
    void receive(String topic, String payload, Integer qos, boolean retained);

    void publish(String topic, String payload, Integer qos, boolean retained);
}
