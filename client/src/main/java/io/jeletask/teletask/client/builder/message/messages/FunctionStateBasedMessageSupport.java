package io.jeletask.teletask.client.builder.message.messages;

import io.jeletask.teletask.client.builder.composer.config.configurables.FunctionConfigurable;
import io.jeletask.teletask.model.spec.ClientConfigSpec;
import io.jeletask.teletask.model.spec.Function;

public abstract class FunctionStateBasedMessageSupport extends FunctionBasedMessageSupport {
    private final String state;

    protected FunctionStateBasedMessageSupport(ClientConfigSpec clientConfig, Function function, String state) {
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
