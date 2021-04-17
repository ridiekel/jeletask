package io.github.ridiekel.jeletask.client.builder.message.strategy;

import io.github.ridiekel.jeletask.client.TeletaskClientImpl;
import io.github.ridiekel.jeletask.model.spec.Function;

public interface GroupGetStrategy {
    void execute(TeletaskClientImpl client, Function function, int... numbers);
}
