package io.github.ridiekel.jeletask.client;

import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;

@FunctionalInterface
public interface SuccessConsumer {
    void execute(Function function, int number, State<?> state);
}
