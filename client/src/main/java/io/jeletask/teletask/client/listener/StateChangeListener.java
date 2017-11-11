package io.jeletask.teletask.client.listener;

import io.jeletask.teletask.model.spec.ComponentSpec;

import java.util.List;

public interface StateChangeListener {
    void receive(List<ComponentSpec> components);

    void stop();
}
