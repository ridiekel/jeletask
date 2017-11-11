package io.jeletask.client.builder.message.strategy;

import io.jeletask.client.TeletaskClient;

public interface KeepAliveStrategy {
    int getIntervalMinutes();

    void execute(TeletaskClient client) throws Exception;
}
