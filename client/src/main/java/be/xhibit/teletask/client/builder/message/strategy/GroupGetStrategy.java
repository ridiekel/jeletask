package be.xhibit.teletask.client.builder.message.strategy;

import be.xhibit.teletask.client.TeletaskClient;
import be.xhibit.teletask.model.spec.Function;

public interface GroupGetStrategy {
    void execute(TeletaskClient client, Function function, int... numbers) throws Exception;
}
