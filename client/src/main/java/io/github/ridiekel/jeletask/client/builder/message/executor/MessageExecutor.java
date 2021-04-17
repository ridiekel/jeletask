package io.github.ridiekel.jeletask.client.builder.message.executor;

import io.github.ridiekel.jeletask.client.TeletaskClientImpl;
import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;

public class MessageExecutor implements Runnable {
    private final MessageSupport message;
    private final TeletaskClientImpl client;

    public MessageExecutor(MessageSupport message, TeletaskClientImpl client) {
        this.message = message;
        this.client = client;
    }

    @Override
    public void run() {
        this.message.execute(this.client);
    }
}
