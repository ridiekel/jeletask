package io.github.ridiekel.jeletask.mqtt.listener.tracing.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "sba.server", name = "enabled", havingValue = "false", matchIfMissing = true)
public class MqttMessageTraceServiceNoOpImpl implements MqttMessageTraceService {
    @Override
    public void receive(String topic, String payload, Integer qos, boolean retained) {

    }

    @Override
    public void publish(String topic, String payload, Integer qos, boolean retained) {

    }
}
