package io.github.ridiekel.jeletask.client.builder.composer.config.configurables;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.config.Configurable;
import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class CommandConfigurable<M extends MessageSupport> extends Configurable<Command> {
    private final Map<Integer, String> paramNames;
    private final boolean needsCentralUnitParameter;

    public CommandConfigurable(Command command, int number, boolean needsCentralUnitParameter, String... paramNames) {
        super(number, command);

        AtomicInteger index = new AtomicInteger(0);
        this.paramNames = Stream.of(paramNames).collect(Collectors.toMap(p -> index.getAndIncrement(), Function.identity()));
        this.needsCentralUnitParameter = needsCentralUnitParameter;
    }

    public Map<Integer, String> getParamNames() {
        return this.paramNames;
    }

    public boolean needsCentralUnitParameter() {
        return this.needsCentralUnitParameter;
    }

    public abstract M parse(CentralUnit centralUnit, MessageHandler messageHandler, byte[] rawBytes, byte[] payload);

    public int getOutputNumber(MessageHandler messageHandler, byte[] payload, int fromByte) {
        byte[] output = new byte[4];
        System.arraycopy(payload, fromByte, output, 4 - messageHandler.getOutputByteSize(), messageHandler.getOutputByteSize());
        return ByteBuffer.wrap(output).getInt();
    }
}
