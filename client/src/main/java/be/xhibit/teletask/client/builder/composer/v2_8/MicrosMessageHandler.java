package be.xhibit.teletask.client.builder.composer.v2_8;

import be.xhibit.teletask.client.TeletaskClient;
import be.xhibit.teletask.client.builder.composer.MessageHandlerSupport;
import be.xhibit.teletask.client.builder.message.executor.MessageExecutor;
import be.xhibit.teletask.client.builder.message.messages.impl.EventMessage;
import be.xhibit.teletask.client.builder.message.messages.impl.GetMessage;
import be.xhibit.teletask.client.builder.message.messages.impl.LogMessage;
import be.xhibit.teletask.client.builder.message.strategy.GroupGetStrategy;
import be.xhibit.teletask.client.builder.message.strategy.KeepAliveStrategy;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;
import be.xhibit.teletask.model.spec.Function;
import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;

import java.util.List;

public class MicrosMessageHandler extends MessageHandlerSupport {
    public static final MicrosKeepAliveStrategy KEEP_ALIVE_STRATEGY = new MicrosKeepAliveStrategy();
    public static final MicrosGroupGetStrategy GROUP_GET_STRATEGY = new MicrosGroupGetStrategy();

    public MicrosMessageHandler() {
        super(new MicrosCommandConfiguration(), new MicrosFunctionConfiguration());
    }

    @Override
    public byte[] compose(Command command, byte[] payload) {
        int msgStx = this.getStxValue();                                    // STX: This is the value indicating the start of a command/event
        int msgLength = 3 + payload.length;                                 // Length: the length of the command without checksum
        int msgCommand = this.getCommandConfig(command).getNumber();        // Command Number

        byte[] messageBytes = Bytes.concat(new byte[]{(byte) msgStx, (byte) msgLength, (byte) msgCommand}, payload);

        return this.addCheckSum(messageBytes);
    }

    @Override
    public byte[] composeOutput(int... numbers) {
        byte[] outputs = new byte[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            outputs[i] = (byte) numbers[i];

        }
        return outputs;
    }

    @Override
    public EventMessage parseEvent(ClientConfigSpec config, byte[] message) {
        //02 09 10 01 03 00 31
        int counter = 2; //We skip first 3 since they are of no use to us at this time.
        Function function = this.getFunction(message[++counter]);
        int number = message[++counter];

        String state = this.parseState(message, ++counter, config, function, number);

        return new EventMessage(config, message, function, number, state);
    }

    @Override
    public KeepAliveStrategy getKeepAliveStrategy() {
        return KEEP_ALIVE_STRATEGY;
    }

    @Override
    public GroupGetStrategy getGroupGetStrategy() {
        return GROUP_GET_STRATEGY;
    }

    @Override
    public int getOutputByteSize() {
        return 1;
    }

    @Override
    public List<EventMessage> createResponseEventMessage(ClientConfigSpec config, Function function, OutputState... numbers) {
        OutputState outputState = numbers[0];

        byte[] rawBytes = new byte[]{(byte) this.getStxValue(), 0};
        rawBytes = Bytes.concat(rawBytes, this.getCommandConfig(Command.EVENT).getBytes());
        rawBytes = Bytes.concat(rawBytes, this.getFunctionConfig(function).getBytes());
        rawBytes = Bytes.concat(rawBytes, new byte[]{(byte) outputState.getNumber()});
        rawBytes = Bytes.concat(rawBytes, this.getStateBytes(config, function, outputState));
        rawBytes = Bytes.concat(rawBytes, new byte[]{0}); // The furture checksum

        this.setLengthAndCheckSum(rawBytes);

        return Lists.newArrayList(new EventMessage(config, rawBytes, function, outputState.getNumber(), outputState.getState()));
    }

    private static class MicrosKeepAliveStrategy implements KeepAliveStrategy {
        @Override
        public int getIntervalMinutes() {
            return 30;
        }

        @Override
        public void execute(TeletaskClient client) throws Exception {
            new MessageExecutor(new LogMessage(client.getConfig(), Function.MOTOR, "ON"), client).run();
        }
    }

    private static class MicrosGroupGetStrategy implements GroupGetStrategy {
        @Override
        public void execute(TeletaskClient client, Function function, int... numbers) throws Exception {
            // For some reason the microsplus does not always send an event after requesting the state of a component.
            // As a workaround, we keep trying until we get the state of all components.
            // Sleeping between get messages seems to decrease the amount of failures.
            // Problem with this approach is that we have no idea when the server actually will be able to completely start.
            while (this.stateEmptyCount(client.getConfig(), function, numbers) > 0) {
                for (int number : numbers) {
                    if (client.getConfig().getComponent(function, number).getState() == null) {
                        new MessageExecutor(new GetMessage(client.getConfig(), function, number), client).run();
                        Thread.sleep(150);
                    }
                }
            }
        }

        private int stateEmptyCount(ClientConfigSpec config, Function function, int... numbers) {
            int counter = 0;
            for (int number : numbers) {
                counter += config.getComponent(function, number).getState() == null ? 1 : 0;
            }
            return counter;
        }
    }
}
