package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.builder.message.messages.GetMessageSupport;
import io.github.ridiekel.jeletask.model.spec.CentralUnit;
import io.github.ridiekel.jeletask.model.spec.Function;

public class GetMessage extends GetMessageSupport {
    public GetMessage(CentralUnit clientConfig, Function function, int number) {
        super(function, clientConfig, number);
    }
}
