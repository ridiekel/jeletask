package io.github.ridiekel.jeletask.client.builder.message.strategy;

import io.github.ridiekel.jeletask.client.TeletaskClient;
import io.github.ridiekel.jeletask.model.spec.Function;

public interface GroupGetStrategy {
    void execute(TeletaskClient client, Function function, int... numbers) throws Exception;
}
