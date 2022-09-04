package io.github.ridiekel.jeletask.client.builder.message.messages;

import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.FunctionConfigurable;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

public abstract class FunctionStateBasedMessageSupport extends FunctionBasedMessageSupport {
    private final ComponentState state;

    protected FunctionStateBasedMessageSupport(CentralUnit clientConfig, Function function, ComponentState state) {
        super(clientConfig, function);
        this.state = state;
        this.state.setState(this.state.getState().toUpperCase());
    }

    public ComponentState getState() {
        return this.state;
    }

    @Override
    protected boolean isValid() {
        FunctionConfigurable functionConfig = this.getMessageHandler().getFunctionConfig(this.getFunction());
        return functionConfig.isValidState(this.getState());
    }
}
