package io.jeletask.teletask.client.builder.composer.v2_8;

import io.jeletask.teletask.client.builder.composer.MessageHandler;
import io.jeletask.teletask.client.builder.composer.config.ConfigurationSupport;
import io.jeletask.teletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.jeletask.teletask.client.builder.composer.config.configurables.command.EventCommandConfigurable;
import io.jeletask.teletask.client.builder.composer.config.configurables.command.GetCommandConfigurable;
import io.jeletask.teletask.client.builder.composer.config.configurables.command.LogCommandConfigurable;
import io.jeletask.teletask.client.builder.composer.config.configurables.command.SetCommandConfigurable;
import io.jeletask.teletask.client.builder.message.messages.impl.EventMessage;
import io.jeletask.teletask.client.builder.message.messages.impl.GetMessage;
import io.jeletask.teletask.client.builder.message.messages.impl.SetMessage;
import io.jeletask.teletask.model.spec.ClientConfigSpec;
import io.jeletask.teletask.model.spec.Command;
import io.jeletask.teletask.model.spec.Function;

import java.util.List;

public class MicrosCommandConfiguration extends ConfigurationSupport<Command, CommandConfigurable<?>, Integer> {
    public MicrosCommandConfiguration() {
        super(List.of(
                new MicrosSetCommandConfigurable(),
                new MicrosGetCommandConfigurable(),
                new LogCommandConfigurable(3, false, "Fnc", "Sate"),
                new MicrosEventCommandConfigurable())
        );
    }

    @Override
    protected Integer getKey(CommandConfigurable<?> configurable) {
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
