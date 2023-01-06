package io.github.ridiekel.jeletask.client.builder.composer.microsplus;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.config.ConfigurationSupport;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command.EventCommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command.GetCommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command.GroupGetCommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command.KeepAliveCommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command.LogCommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command.SetCommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command.DisplayMessageCommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.EventMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.GetMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.SetMessage;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

import java.util.List;

public class MicrosPlusCommandConfiguration extends ConfigurationSupport<Command, CommandConfigurable<?>, Integer> {
    public MicrosPlusCommandConfiguration() {
        super(List.of(
                new MicrosPlusSetCommandConfigurable(),
                new MicrosPlusGetCommandConfigurable(),
                new GroupGetCommandConfigurable(9, true, "Central Unit", "Fnc", "Number", "Number"),
                new LogCommandConfigurable(3, false, "Fnc", "State"),
                new MicrosPlusEventCommandConfigurable(),
                new KeepAliveCommandConfigurable(11, true),
                new DisplayMessageCommandConfigurable(4, true, "Central Unit")            
        ));
    }

    @Override
    protected Integer getKey(CommandConfigurable<?> configurable) {
        return configurable.getNumber();
    }

    private static class MicrosPlusEventCommandConfigurable extends EventCommandConfigurable {
        public MicrosPlusEventCommandConfigurable() {
            super(16, true, "Central Unit", "Fnc", "Number", "Number", "Err State");
        }

        @Override
        public EventMessage parse(CentralUnit centralUnit, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
            Function function = messageHandler.getFunction(payload[1]);
            int number = this.getOutputNumber(messageHandler, payload, 2);
            ComponentState state = getState(messageHandler, centralUnit, function, number, payload, 5);
            return new EventMessage(centralUnit, rawBytes, function, number, state);
        }
    }

    private static class MicrosPlusGetCommandConfigurable extends GetCommandConfigurable {
        public MicrosPlusGetCommandConfigurable() {
            super(6, true, "Central Unit", "Fnc", "Number", "Number");
        }

        @Override
        public GetMessage parse(CentralUnit centralUnit, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
            return new GetMessage(centralUnit, messageHandler.getFunction(payload[1]), this.getOutputNumber(messageHandler, payload, 2));
        }
    }

    private static class MicrosPlusSetCommandConfigurable extends SetCommandConfigurable {
        public MicrosPlusSetCommandConfigurable() {
            super(7, true, "Central Unit", "Fnc", "Number", "Number", "State");
        }

        @Override
        public SetMessage parse(CentralUnit centralUnit, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
            Function function = messageHandler.getFunction(payload[1]);
            int number = this.getOutputNumber(messageHandler, payload, 2);
            ComponentState state = getState(messageHandler, centralUnit, function, number, payload, messageHandler.getOutputByteSize() + 2);
            return new SetMessage(centralUnit, function, number, state);
        }
    }
}
