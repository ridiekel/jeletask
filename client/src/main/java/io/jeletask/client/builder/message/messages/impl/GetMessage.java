package io.jeletask.client.builder.message.messages.impl;

import io.jeletask.client.builder.message.messages.GetMessageSupport;
import io.jeletask.model.spec.ClientConfigSpec;
import io.jeletask.model.spec.Function;

public class GetMessage extends GetMessageSupport {
    public GetMessage(ClientConfigSpec clientConfig, Function function, int number) {
        super(function, clientConfig, number);
    }
}
