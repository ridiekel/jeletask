package io.jeletask.client.builder.message.messages;

import io.jeletask.model.spec.ClientConfigSpec;
import io.jeletask.model.spec.Function;

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
