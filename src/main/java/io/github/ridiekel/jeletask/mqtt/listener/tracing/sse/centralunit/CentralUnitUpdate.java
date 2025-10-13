package io.github.ridiekel.jeletask.mqtt.listener.tracing.sse.centralunit;

import io.github.ridiekel.jeletask.client.spec.Function;

public record CentralUnitUpdate(
        Function componentType,
        Integer number,
        Object state
) {
}
