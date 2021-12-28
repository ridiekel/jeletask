package io.github.ridiekel.jeletask.client.builder.composer.config.configurables.command;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.CommandConfigurable;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.GroupGetMessage;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;

import java.nio.ByteBuffer;

public class GroupGetCommandConfigurable extends CommandConfigurable<GroupGetMessage> {
    public GroupGetCommandConfigurable(int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(Command.GROUPGET, number, needsCentralUnitParameter, paramNames);
    }

    @Override
    public GroupGetMessage parse(CentralUnit config, MessageHandler messageHandler, byte[] rawBytes, byte[] payload) {
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
