package io.jeletask.teletask.client.builder.message.messages;

import io.jeletask.teletask.model.spec.ClientConfigSpec;
import io.jeletask.teletask.model.spec.Function;

public abstract class FunctionBasedMessageSupport extends MessageSupport {
    private final Function function;

    protected FunctionBasedMessageSupport(ClientConfigSpec clientConfig, Function function) {
        super(clientConfig);
        this.function = function;
    }

    public Function getFunction() {
        return this.function;
    }
}
