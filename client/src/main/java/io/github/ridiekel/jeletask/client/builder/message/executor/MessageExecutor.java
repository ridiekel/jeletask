package io.github.ridiekel.jeletask.client.builder.message.executor;

import io.github.ridiekel.jeletask.client.TeletaskClientImpl;
import io.github.ridiekel.jeletask.client.builder.message.messages.AcknowledgeException;
import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageExecutor implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(MessageExecutor.class);
    private final MessageSupport message;
    private final TeletaskClientImpl client;

    public MessageExecutor(MessageSupport message, TeletaskClientImpl client) {
        this.message = message;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            this.message.execute(this.client);
        } catch (Exception e) {
            LOG.error(String.format("Problem running message: %s", this.message), e);
            throw new TeletaskClientImpl.CommunicationException("Problem running message", e);
        }
    }
}
