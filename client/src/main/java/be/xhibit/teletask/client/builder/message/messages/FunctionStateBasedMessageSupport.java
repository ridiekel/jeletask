package be.xhibit.teletask.client.builder.message.messages;

import be.xhibit.teletask.client.builder.composer.config.configurables.FunctionConfigurable;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Function;

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
