package io.github.ridiekel.jeletask;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.model.spec.CentralUnit;

import java.io.InputStream;

public interface TeletaskReceiver {
    InputStream getInputStream();

    MessageHandler getMessageHandler();

    CentralUnit getConfig();
}
