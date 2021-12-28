package io.github.ridiekel.jeletask.client.listener;

import io.github.ridiekel.jeletask.client.spec.ComponentSpec;

import java.util.List;

public interface StateChangeListener {
    void receive(List<ComponentSpec> components);

    void stop();
}
