package io.jeletask.client.builder.message.strategy;

import io.jeletask.client.TeletaskClient;
import io.jeletask.model.spec.Function;

public interface GroupGetStrategy {
    void execute(TeletaskClient client, Function function, int... numbers) throws Exception;
}
