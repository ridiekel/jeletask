package io.jeletask.teletask.client.builder.composer.config.configurables.command;

import io.jeletask.teletask.client.builder.composer.MessageHandler;
import io.jeletask.teletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.jeletask.teletask.client.builder.message.messages.impl.GroupGetMessage;
import io.jeletask.teletask.model.spec.ClientConfigSpec;
import io.jeletask.teletask.model.spec.Command;

import java.nio.ByteBuffer;

public class GroupGetCommandConfigurable extends CommandConfigurable<GroupGetMessage> {
    public GroupGetCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.GROUPGET, number, needsCentralUnitParameter, paramNames);
    }

    @Override
    public GroupGetMessage parse(ClientConfigSpec config, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
        int outputByteSize = messageHandler.getOutputByteSize();

        int[] numbers = new int[(payload.length - 2) / outputByteSize];

        int numberCounter = 0;
        for (int i = 2; i < payload.length; i += outputByteSize) {
            byte[] bytes = new byte[4];
            System.arraycopy(payload, i, bytes, 4 - outputByteSize, outputByteSize);
            numbers[numberCounter++] = ByteBuffer.wrap(bytes).getInt();
        }

        return new GroupGetMessage(config, messageHandler.getFunction(payload[1]), numbers);
    }
}
