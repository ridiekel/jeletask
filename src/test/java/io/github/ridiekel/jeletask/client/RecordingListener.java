package io.github.ridiekel.jeletask.client;

import io.github.ridiekel.jeletask.client.listener.StateChangeListener;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class RecordingListener implements StateChangeListener {
    private final List<ComponentSpec> received = new CopyOnWriteArrayList<>();
    private final AtomicInteger stops = new AtomicInteger();
    private volatile boolean failing;

    public void failOnEverything() {
        this.failing = true;
    }

    public void clear() {
        this.received.clear();
    }

    public List<ComponentSpec> received() {
        return List.copyOf(this.received);
    }

    public int stops() {
        return this.stops.get();
    }

    @Override
    public void receive(List<ComponentSpec> components) {
        this.received.addAll(components);
        failIfAsked();
    }

    @Override
    public void stop() {
        this.stops.incrementAndGet();
        failIfAsked();
    }

    private void failIfAsked() {
        if (this.failing) {
            throw new IllegalStateException("Listener failure");
        }
    }
}
