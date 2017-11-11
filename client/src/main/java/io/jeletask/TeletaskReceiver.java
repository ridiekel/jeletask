package io.jeletask;

import io.jeletask.client.builder.composer.MessageHandler;
import io.jeletask.model.spec.ClientConfigSpec;

import java.io.InputStream;

public interface TeletaskReceiver {
    InputStream getInputStream();

    MessageHandler getMessageHandler();

    ClientConfigSpec getConfig();
}
