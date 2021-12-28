package io.github.ridiekel.jeletask.client.builder.composer.v3_1;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.config.ConfigurationSupport;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command.EventCommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command.GetCommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command.GroupGetCommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command.KeepAliveCommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command.LogCommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command.SetCommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.EventMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.GetMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.SetMessage;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.Function;

import java.util.List;

public class MicrosPlusCommandConfiguration extends ConfigurationSupport<Command, CommandConfigurable<?>, Integer> {
    public MicrosPlusCommandConfiguration() {
        super(List.of(
                new MicrosPlusSetCommandConfigurable(),
                new MicrosPlusGetCommandConfigurable(),
                new GroupGetCommandConfigurable(9, true, "Central Unit", "Fnc", "Output Part 1", "Output Part 2"),
                new LogCommandConfigurable(3, false, "Fnc", "State"),
                new MicrosPlusEventCommandConfigurable(),
                new KeepAliveCommandConfigurable(11, true)
        ));
    }

    @Override
    protected Integer getKey(CommandConfigurable<?> configurable) {
        return configurable.getNumber();
    }

    private static class MicrosPlusEventCommandConfigurable extends EventCommandConfigurable {
        public MicrosPlusEventCommandConfigurable() {
            super(16, true, "Central Unit", "Fnc", "Output Part 1", "Output Part 2", "Err State", "State", "State");
        }

        @Override
        public EventMessage parse(CentralUnit config, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
            Function function = messageHandler.getFunction(payload[1]);
            int number = this.getOutputNumber(messageHandler, payload, 2);
            String state = getState(messageHandler, config, function, number, payload, 5);
            return new EventMessage(config, rawBytes, function, number, state);
        }
    }

    private static class MicrosPlusGetCommandConfigurable extends GetCommandConfigurable {
        public MicrosPlusGetCommandConfigurable() {
            super(6, true, "Central Unit", "Fnc", "Output Part 1", "Output Part 2");
        }

        @Override
        public GetMessage parse(CentralUnit config, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
            return new GetMessage(config, messageHandler.getFunction(payload[1]), this.getOutputNumber(messageHandler, payload, 2));
        }
    }

    private static class MicrosPlusSetCommandConfigurable extends SetCommandConfigurable {
        public MicrosPlusSetCommandConfigurable() {
            super(7, true, "Central Unit", "Fnc", "Output Part 1", "Output Part 2", "State");
        }

        @Override
        public SetMessage parse(CentralUnit config, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
            Function function = messageHandler.getFunction(payload[1]);
            int number = this.getOutputNumber(messageHandler, payload, 2);
            String state = getState(messageHandler, config, function, number, payload, messageHandler.getOutputByteSize() + 2);
            return new SetMessage(config, function, number, state);
        }
    }
}
