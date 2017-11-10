package be.xhibit.teletask.client.builder.message.messages.impl;

import be.xhibit.teletask.client.builder.message.messages.GetMessageSupport;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Function;

public class GetMessage extends GetMessageSupport {
    public GetMessage(ClientConfigSpec clientConfig, Function function, int number) {
        super(function, clientConfig, number);
    }
}
