package io.jeletask.teletask.client.builder.message.messages.impl;

import io.jeletask.teletask.client.builder.message.messages.GetMessageSupport;
import io.jeletask.teletask.model.spec.ClientConfigSpec;
import io.jeletask.teletask.model.spec.Function;

public class GetMessage extends GetMessageSupport {
    public GetMessage(ClientConfigSpec clientConfig, Function function, int number) {
        super(function, clientConfig, number);
    }
}
