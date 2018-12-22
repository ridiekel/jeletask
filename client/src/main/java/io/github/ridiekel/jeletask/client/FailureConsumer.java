package io.github.ridiekel.jeletask.client;

import io.github.ridiekel.jeletask.model.spec.Function;

@FunctionalInterface
public interface FailureConsumer {
    void execute(Function function, int number, String state, Exception e);
}
