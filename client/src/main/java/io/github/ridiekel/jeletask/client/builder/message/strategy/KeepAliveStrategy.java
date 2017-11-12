package io.github.ridiekel.jeletask.client.builder.message.strategy;

import io.github.ridiekel.jeletask.client.TeletaskClient;

public interface KeepAliveStrategy {
    int getIntervalMinutes();

    void execute(TeletaskClient client) throws Exception;
}
