package io.github.ridiekel.jeletask.client.builder.message.messages;

import io.github.ridiekel.jeletask.model.spec.CentralUnit;
import io.github.ridiekel.jeletask.model.spec.Function;

public abstract class FunctionBasedMessageSupport extends MessageSupport {
    private final Function function;

    protected FunctionBasedMessageSupport(CentralUnit clientConfig, Function function) {
        super(clientConfig);
        this.function = function;
    }

    public Function getFunction() {
        return this.function;
    }
}
