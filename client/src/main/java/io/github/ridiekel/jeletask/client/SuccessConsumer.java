package io.github.ridiekel.jeletask.client;

import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

@FunctionalInterface
public interface SuccessConsumer {
    void execute(Function function, int number, ComponentState state);
}
