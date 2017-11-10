package be.xhibit.teletask.client.builder.composer.v3_1;

import be.xhibit.teletask.client.builder.composer.MessageHandler;
import be.xhibit.teletask.client.builder.composer.config.ConfigurationSupport;
import be.xhibit.teletask.client.builder.composer.config.configurables.CommandConfigurable;
import be.xhibit.teletask.client.builder.composer.config.configurables.command.EventCommandConfigurable;
import be.xhibit.teletask.client.builder.composer.config.configurables.command.GetCommandConfigurable;
import be.xhibit.teletask.client.builder.composer.config.configurables.command.GroupGetCommandConfigurable;
import be.xhibit.teletask.client.builder.composer.config.configurables.command.KeepAliveCommandConfigurable;
import be.xhibit.teletask.client.builder.composer.config.configurables.command.LogCommandConfigurable;
import be.xhibit.teletask.client.builder.composer.config.configurables.command.SetCommandConfigurable;
import be.xhibit.teletask.client.builder.message.messages.impl.EventMessage;
import be.xhibit.teletask.client.builder.message.messages.impl.GetMessage;
import be.xhibit.teletask.client.builder.message.messages.impl.SetMessage;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;
import be.xhibit.teletask.model.spec.Function;
import com.google.common.collect.ImmutableList;

public class MicrosPlusCommandConfiguration extends ConfigurationSupport<Command, CommandConfigurable<?>, Integer> {
    public MicrosPlusCommandConfiguration() {
        super(ImmutableList.<CommandConfigurable<?>>builder()
                .add(new MicrosPlusSetCommandConfigurable())
                .add(new MicrosPlusGetCommandConfigurable())
                .add(new GroupGetCommandConfigurable(9, true, "Central Unit", "Fnc", "Output Part 1", "Output Part 2"))
                .add(new LogCommandConfigurable(3, false, "Fnc", "State"))
                .add(new MicrosPlusEventCommandConfigurable())
                .add(new KeepAliveCommandConfigurable(11, true))
                .build());
    }

    @Override
    protected Integer getKey(CommandConfigurable configurable) {
        return configurable.getNumber();
    }

    private static class MicrosPlusEventCommandConfigurable extends EventCommandConfigurable {
        public MicrosPlusEventCommandConfigurable() {
            super(16, true, "Central Unit", "Fnc", "Output Part 1", "Output Part 2", "Err State", "State", "State");
        }

        @Override
        public EventMessage parse(ClientConfigSpec config, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
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
        public GetMessage parse(ClientConfigSpec config, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
            return new GetMessage(config, messageHandler.getFunction(payload[1]), this.getOutputNumber(messageHandler, payload, 2));
        }
    }

    private static class MicrosPlusSetCommandConfigurable extends SetCommandConfigurable {
        public MicrosPlusSetCommandConfigurable() {
            super(7, true, "Central Unit", "Fnc", "Output Part 1", "Output Part 2", "State");
        }

        @Override
        public SetMessage parse(ClientConfigSpec config, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
            Function function = messageHandler.getFunction(payload[1]);
            int number = this.getOutputNumber(messageHandler, payload, 2);
            String state = getState(messageHandler, config, function, number, payload, messageHandler.getOutputByteSize() + 2);
            return new SetMessage(config, function, number, state);
        }
    }
}
