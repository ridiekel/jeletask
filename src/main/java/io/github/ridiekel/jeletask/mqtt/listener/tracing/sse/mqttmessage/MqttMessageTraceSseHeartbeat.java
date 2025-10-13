package io.github.ridiekel.jeletask.mqtt.listener.tracing.sse.mqttmessage;

import org.springframework.stereotype.Component;

@Component
class MqttMessageTraceSseHeartbeat {
    private final MqttMessageTraceSseController sse;

    MqttMessageTraceSseHeartbeat(MqttMessageTraceSseController sse) {
        this.sse = sse;
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 5000)
    public void ping() {
        sse.keepAlive();
    }
}
