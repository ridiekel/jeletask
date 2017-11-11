package io.jeletask.teletask.client.builder.message.strategy;

import io.jeletask.teletask.client.TeletaskClient;
import io.jeletask.teletask.model.spec.Function;

public interface GroupGetStrategy {
    void execute(TeletaskClient client, Function function, int... numbers) throws Exception;
}
