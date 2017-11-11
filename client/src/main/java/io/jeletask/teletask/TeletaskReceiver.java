package io.jeletask.teletask;

import io.jeletask.teletask.client.builder.composer.MessageHandler;
import io.jeletask.teletask.model.spec.ClientConfigSpec;

import java.io.InputStream;

public interface TeletaskReceiver {
    InputStream getInputStream();

    MessageHandler getMessageHandler();

    ClientConfigSpec getConfig();
}
