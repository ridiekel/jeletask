package io.github.ridiekel.jeletask.client.builder.message.messages;

import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.FunctionConfigurable;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Function;

public abstract class FunctionStateBasedMessageSupport extends FunctionBasedMessageSupport {
    private final String state;

    protected FunctionStateBasedMessageSupport(CentralUnit clientConfig, Function function, String state) {
        super(clientConfig, function);
        this.state = state.toUpperCase();
    }

    public String getState() {
        return this.state;
    }

    @Override
    protected boolean isValid() {
        FunctionConfigurable functionConfig = this.getMessageHandler().getFunctionConfig(this.getFunction());
        return functionConfig.isValidState(this.getState());
    }
}
