package be.xhibit.teletask.client.builder.message.strategy;

import be.xhibit.teletask.client.TeletaskClient;

public interface KeepAliveStrategy {
    int getIntervalMinutes();

    void execute(TeletaskClient client) throws Exception;
}
