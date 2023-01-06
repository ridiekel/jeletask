package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.builder.message.messages.GetMessageSupport;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Function;

public class GetMessage extends GetMessageSupport {
    public GetMessage(CentralUnit centralUnit, Function function, int number) {
        super(function, centralUnit, number);
    }
}
