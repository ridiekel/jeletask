package be.xhibit.teletask.client.builder.composer.v2_8;

import be.xhibit.teletask.client.builder.composer.MessageHandler;
import be.xhibit.teletask.client.builder.composer.config.ConfigurationSupport;
import be.xhibit.teletask.client.builder.composer.config.configurables.CommandConfigurable;
import be.xhibit.teletask.client.builder.composer.config.configurables.command.EventCommandConfigurable;
import be.xhibit.teletask.client.builder.composer.config.configurables.command.GetCommandConfigurable;
import be.xhibit.teletask.client.builder.composer.config.configurables.command.LogCommandConfigurable;
import be.xhibit.teletask.client.builder.composer.config.configurables.command.SetCommandConfigurable;
import be.xhibit.teletask.client.builder.message.messages.impl.EventMessage;
import be.xhibit.teletask.client.builder.message.messages.impl.GetMessage;
import be.xhibit.teletask.client.builder.message.messages.impl.SetMessage;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;
import be.xhibit.teletask.model.spec.Function;
import com.google.common.collect.ImmutableList;

public class MicrosCommandConfiguration extends ConfigurationSupport<Command, CommandConfigurable<?>, Integer> {
    public MicrosCommandConfiguration() {
        super(ImmutableList.<CommandConfigurable<?>>builder()
                .add(new MicrosSetCommandConfigurable())
                .add(new MicrosGetCommandConfigurable())
                .add(new LogCommandConfigurable(3, false, "Fnc", "Sate"))
                .add(new MicrosEventCommandConfigurable())
                .build());
    }

    @Override
    protected Integer getKey(CommandConfigurable configurable) {
        return configurable.getNumber();
    }

    private static class MicrosEventCommandConfigurable extends EventCommandConfigurable {
        public MicrosEventCommandConfigurable() {
            super(8, false, "Fnc", "Output", "State");
        }

        @Override
        public EventMessage parse(ClientConfigSpec config, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
            Function function = messageHandler.getFunction(payload[0]);
            int number = this.getOutputNumber(messageHandler, payload, 1);
            String state = getState(messageHandler, config, function, number, payload, 2);
            return new EventMessage(config, rawBytes, function, number, state);
        }
    }

    private static class MicrosGetCommandConfigurable extends GetCommandConfigurable {
        public MicrosGetCommandConfigurable() {
            super(2, false, "Fnc", "Output");
        }

        @Override
        public GetMessage parse(ClientConfigSpec config, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
            return new GetMessage(config, messageHandler.getFunction(payload[0]), this.getOutputNumber(messageHandler, payload, 1));
        }
    }

    private static class MicrosSetCommandConfigurable extends SetCommandConfigurable {
        public MicrosSetCommandConfigurable() {
            super(1, false, "Fnc", "Output", "State");
        }

        @Override
        public SetMessage parse(ClientConfigSpec config, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
            Function function = messageHandler.getFunction(payload[0]);
            int number = this.getOutputNumber(messageHandler, payload, 1);
            String state = getState(messageHandler, config, function, number, payload, messageHandler.getOutputByteSize() + 1);
            return new SetMessage(config, function, number, state);
        }
    }
}
