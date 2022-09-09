package io.github.ridiekel.jeletask.client.builder.message.messages;

import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

import java.util.Optional;

public abstract class FunctionStateBasedMessageSupport extends FunctionBasedMessageSupport {
    private final ComponentState state;

    protected FunctionStateBasedMessageSupport(CentralUnit clientConfig, Function function, ComponentState state) {
        super(clientConfig, function);
        this.state = state;
        if (state != null) {
            this.state.setState(Optional.ofNullable(state.getState()).map(String::toUpperCase).orElse(null));
        }
    }

    public ComponentState getState() {
        return this.state;
    }
}
