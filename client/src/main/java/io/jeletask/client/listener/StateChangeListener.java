package io.jeletask.client.listener;

import io.jeletask.model.spec.ComponentSpec;

import java.util.List;

public interface StateChangeListener {
    void receive(List<ComponentSpec> components);

    void stop();
}
