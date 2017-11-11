package io.jeletask.client.builder.message.executor;

import io.jeletask.client.TeletaskClient;
import io.jeletask.client.builder.message.messages.MessageSupport;

public class MessageExecutor implements Runnable {
    private final MessageSupport message;
    private final TeletaskClient client;

    public MessageExecutor(MessageSupport message, TeletaskClient client) {
        this.message = message;
        this.client = client;
    }

    @Override
    public void run() {
        this.message.execute(this.client);
    }
}
