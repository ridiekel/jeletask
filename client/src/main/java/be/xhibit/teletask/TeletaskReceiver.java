package be.xhibit.teletask;

import be.xhibit.teletask.client.builder.composer.MessageHandler;
import be.xhibit.teletask.model.spec.ClientConfigSpec;

import java.io.InputStream;

public interface TeletaskReceiver {
    InputStream getInputStream();

    MessageHandler getMessageHandler();

    ClientConfigSpec getConfig();
}
