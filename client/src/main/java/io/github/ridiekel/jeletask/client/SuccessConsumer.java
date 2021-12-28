package io.github.ridiekel.jeletask.client;

import io.github.ridiekel.jeletask.client.spec.Function;

@FunctionalInterface
public interface SuccessConsumer {
    void execute(Function function, int number, String state);
}
