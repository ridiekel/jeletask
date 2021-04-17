package io.github.ridiekel.jeletask.client.builder.message.strategy;

import io.github.ridiekel.jeletask.client.TeletaskClientImpl;

public interface KeepAliveStrategy {
    int getIntervalMillis();

    void execute(TeletaskClientImpl client);
}
