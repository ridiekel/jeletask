package io.github.ridiekel.jeletask;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;

import java.io.InputStream;

public interface TeletaskReceiver {
    InputStream getInputStream();

    CentralUnit getCentralUnit();
}
