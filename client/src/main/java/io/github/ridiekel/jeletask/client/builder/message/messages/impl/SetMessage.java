package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.TeletaskClientImpl;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandlerFactory;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.FunctionConfigurable;
import io.github.ridiekel.jeletask.client.builder.message.messages.AcknowledgeException;
import io.github.ridiekel.jeletask.client.builder.message.messages.FunctionStateBasedMessageSupport;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SetMessage extends FunctionStateBasedMessageSupport {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(SetMessage.class);

    private final int number;

    private final byte[] functionBytes;
    private final byte[] outputBytes;
    private final byte[] stateBytes;

    public SetMessage(CentralUnit centralUnit, Function function, int number, ComponentState state) {
        super(centralUnit, function, state);
        this.number = number;

        FunctionConfigurable functionConfig = this.getMessageHandler().getFunctionConfig(this.getFunction());
        ComponentSpec component = this.getCentralUnit().getComponent(this.getFunction(), this.getNumber());
        this.functionBytes = new byte[]{(byte) functionConfig.getNumber()};
        this.outputBytes = MessageHandlerFactory.getMessageHandler(this.getCentralUnit().getCentralUnitType()).composeOutput(this.getNumber());
        this.stateBytes = functionConfig.getStateCalculator(component).toBytes(this.getState());
    }

    public int getNumber() {
        return this.number;
    }

    @Override
    protected byte[] getPayload() {
        return Bytes.concat(functionBytes, outputBytes, stateBytes);
    }

    @Override
    public void execute(TeletaskClientImpl client) throws AcknowledgeException {
        super.execute(client);

//        ComponentSpec component = this.getClientConfig().getComponent(this.getFunction(), this.getNumber());
//        ComponentState initialState = component.getState();
//        long start = System.currentTimeMillis();
//        while (!this.getState().equals(component.getState()) && (System.currentTimeMillis() - start) < 2000) {
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                LOG.trace("Exception ({}) caught in set: {}", e.getClass().getName(), e.getMessage(), e);
//            }
//            try {
//                client.handleReceiveEvents(MessageUtilities.receive(LOG, client));
//            } catch (Exception e) {
//                LOG.error("Exception ({}) caught in execute: {}", e.getClass().getName(), e.getMessage(), e);
//            }
//        }
//        if (this.getFunction().shouldReceiveAcknowledge(this.getState()) && !this.getState().equals(component.getState())) {
//            String message = "Did not receive a state change for " + component.getFunction() + ":" + component.getNumber() + " (" + component.getDescription() + ") within 2 seconds. Assuming failed to set state from '" + initialState + "' to '" + this.getState() + "'";
//            LOG.warn(message);
//        }
    }

    @Override
    protected Command getCommand() {
        return Command.SET;
    }

    @Override
    protected String[] getPayloadLogInfo() {
        return new String[]{
                this.formatFunction(this.getFunction()),
                this.formatOutput(this.getNumber()),
                this.formatState(this.stateBytes, this.getState())
        };
    }

    @Override
    protected String getId() {
        return "SET " + super.getId() + "(" + this.number + ")";
    }

    @Override
    protected boolean isValid() {
        FunctionConfigurable functionConfig = this.getMessageHandler().getFunctionConfig(this.getFunction());
        return functionConfig.isValidState(this.getCentralUnit().getComponent(this.getFunction(), this.getNumber()), this.getState());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        SetMessage that = (SetMessage) o;

        return new EqualsBuilder().appendSuper(super.equals(o)).append(number, that.number).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(number).toHashCode();
    }
}
