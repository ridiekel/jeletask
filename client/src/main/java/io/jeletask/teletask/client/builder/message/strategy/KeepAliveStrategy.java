package io.jeletask.teletask.client.builder.message.strategy;

import io.jeletask.teletask.client.TeletaskClient;

public interface KeepAliveStrategy {
    int getIntervalMinutes();

    void execute(TeletaskClient client) throws Exception;
}
