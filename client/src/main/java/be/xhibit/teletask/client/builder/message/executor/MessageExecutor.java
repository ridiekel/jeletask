package be.xhibit.teletask.client.builder.message.executor;

import be.xhibit.teletask.client.TeletaskClient;
import be.xhibit.teletask.client.builder.message.messages.MessageSupport;

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
