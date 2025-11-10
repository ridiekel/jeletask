package io.github.ridiekel.jeletask.mqtt.listener;

import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.State;

public interface MqttPublisher {
    void publishState(ComponentSpec componentSpec, State<?> state);
}
