package be.xhibit.teletask.client.builder.composer.config.configurables;

import be.xhibit.teletask.client.builder.composer.MessageHandler;
import be.xhibit.teletask.client.builder.composer.config.Configurable;
import be.xhibit.teletask.client.builder.message.messages.MessageSupport;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;
import com.google.common.collect.ImmutableMap;

import java.nio.ByteBuffer;
import java.util.Map;

public abstract class CommandConfigurable<M extends MessageSupport> extends Configurable<Command> {
    private final Map<Integer, String> paramNames;
    private final boolean needsCentralUnitParameter;

    public CommandConfigurable(Command command, int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(number, command);

        ImmutableMap.Builder<Integer, String> builder = ImmutableMap.builder();
        for (int i = 0; i < paramNames.length; i++) {
            String paramName = paramNames[i];
            builder.put(i + 1, paramName);
        }

        this.paramNames = builder.build();
        this.needsCentralUnitParameter = needsCentralUnitParameter;
    }

    public Map<Integer, String> getParamNames() {
        return this.paramNames;
    }

    public boolean needsCentralUnitParameter() {
        return this.needsCentralUnitParameter;
    }

    public abstract M parse(ClientConfigSpec config, MessageHandler messageHandler, byte[] rawBytes, byte[] payload);

    public int getOutputNumber(MessageHandler messageHandler, byte[] payload, int fromByte) {
        byte[] output = new byte[4];
        System.arraycopy(payload, fromByte, output, 4 - messageHandler.getOutputByteSize(), messageHandler.getOutputByteSize());
        return ByteBuffer.wrap(output).getInt();
    }
}
