package be.xhibit.teletask.client.builder.composer.v3_1;

import be.xhibit.teletask.client.TeletaskClient;
import be.xhibit.teletask.client.builder.composer.MessageHandlerSupport;
import be.xhibit.teletask.client.builder.message.executor.MessageExecutor;
import be.xhibit.teletask.client.builder.message.messages.impl.EventMessage;
import be.xhibit.teletask.client.builder.message.messages.impl.GroupGetMessage;
import be.xhibit.teletask.client.builder.message.messages.impl.KeepAliveMessage;
import be.xhibit.teletask.client.builder.message.strategy.GroupGetStrategy;
import be.xhibit.teletask.client.builder.message.strategy.KeepAliveStrategy;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;
import be.xhibit.teletask.model.spec.ComponentSpec;
import be.xhibit.teletask.model.spec.Function;
import be.xhibit.teletask.utilities.Bytes;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MicrosPlusMessageHandler extends MessageHandlerSupport {
    public static final MicrosPlusKeepAliveStrategy KEEP_ALIVE_STRATEGY = new MicrosPlusKeepAliveStrategy();
    public static final MicrosPlusGroupGetStrategy GROUP_GET_STRATEGY = new MicrosPlusGroupGetStrategy();
    public static final int CENTRAL_UNIT = 1;

    public MicrosPlusMessageHandler() {
        super(new MicrosPlusCommandConfiguration(), new MicrosPlusFunctionConfiguration());
    }

    @Override
    public byte[] compose(Command command, byte[] payload) {
        boolean needsCentralUnitParam = this.getCommandConfig(command).needsCentralUnitParameter();

        int msgStx = this.getStxValue();                                    // STX: This is the value indicating the start of a command/event
        int msgLength = (needsCentralUnitParam ? 4 : 3) + payload.length;   // Length: the length of the command without checksum
        int msgCommand = this.getCommandConfig(command).getNumber();        // Command Number
        int msgCentralUnit = CENTRAL_UNIT;                                  // For now, we only support 1 central unit

        byte[] begin = {(byte) msgStx, (byte) msgLength, (byte) msgCommand};
        byte[] messageBytes = null;
        if (needsCentralUnitParam) {
            messageBytes = Bytes.concat(begin, new byte[]{(byte) msgCentralUnit}, payload);
        } else {
            messageBytes = Bytes.concat(begin, payload);
        }

        return this.addCheckSum(messageBytes);
    }

    @Override
    public byte[] composeOutput(int... numbers) {
        byte[] outputs = new byte[numbers.length * 2];
        for (int i = 0; i < numbers.length; i++) {
            byte[] bytes = ByteBuffer.allocate(2).putShort((short) numbers[i]).array();
            outputs[i * 2] = bytes[0];
            outputs[(i * 2) + 1] = bytes[1];
        }
        return outputs;
    }

    @Override
    public EventMessage parseEvent(ClientConfigSpec config, byte[] message) {
        //02 09 10 01 01 00 03 00 00 20
        int counter = 3; //We skip first 4 since they are of no use to us at this time.
        Function function = this.getFunction(message[++counter]);
        int number = ByteBuffer.wrap(new byte[]{message[++counter], message[++counter]}).getShort();
        ++counter; // This is the ErrorState, not used at this time

        String state = this.parseState(message, ++counter, config, function, number);

        return new EventMessage(config, message, function, number, state);
    }

    @Override
    public String getOutputLogHeaderName(int index) {
        return "Output Part " + (((index + 1) % 2) + 1);
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
        return 2;
    }

    @Override
    public List<EventMessage> createResponseEventMessage(ClientConfigSpec config, Function function, OutputState... numbers) {
        List<EventMessage> eventMessages = new ArrayList<>();

        for (OutputState number : numbers) {
            byte[] numberBytes = this.composeOutput(number.getNumber());

            byte[] rawBytes = new byte[]{(byte) this.getStxValue(), 0};
            rawBytes = Bytes.concat(rawBytes, this.getCommandConfig(Command.EVENT).getBytes());
            rawBytes = Bytes.concat(rawBytes, new byte[]{CENTRAL_UNIT});
            rawBytes = Bytes.concat(rawBytes, this.getFunctionConfig(function).getBytes());
            rawBytes = Bytes.concat(rawBytes, numberBytes);
            rawBytes = Bytes.concat(rawBytes, new byte[]{0});
            rawBytes = Bytes.concat(rawBytes, this.getStateBytes(config, function, number));
            rawBytes = Bytes.concat(rawBytes, new byte[]{0});

            this.setLengthAndCheckSum(rawBytes);

            ComponentSpec component = config.getComponent(function, number.getNumber());

            eventMessages.add(new EventMessage(config, rawBytes, function, number.getNumber(), component.getState()));
        }


        return eventMessages;
    }

    private static class MicrosPlusKeepAliveStrategy implements KeepAliveStrategy {
        @Override
        public int getIntervalMinutes() {
            return 3;
        }

        @Override
        public void execute(TeletaskClient client) throws Exception {
            new MessageExecutor(new KeepAliveMessage(client.getConfig()), client).run();
        }
    }

    private static class MicrosPlusGroupGetStrategy implements GroupGetStrategy {
        @Override
        public void execute(TeletaskClient client, Function function, int... numbers) throws Exception {
            new MessageExecutor(new GroupGetMessage(client.getConfig(), function, numbers), client).run();
        }
    }
}
