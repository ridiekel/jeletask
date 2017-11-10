package be.xhibit.teletask.client.builder.composer;

import be.xhibit.teletask.client.builder.ByteUtilities;
import be.xhibit.teletask.client.builder.composer.config.ConfigurationSupport;
import be.xhibit.teletask.client.builder.composer.config.configurables.CommandConfigurable;
import be.xhibit.teletask.client.builder.composer.config.configurables.FunctionConfigurable;
import be.xhibit.teletask.client.builder.message.messages.MessageSupport;
import be.xhibit.teletask.model.spec.ClientConfigSpec;
import be.xhibit.teletask.model.spec.Command;
import be.xhibit.teletask.model.spec.ComponentSpec;
import be.xhibit.teletask.model.spec.Function;
import com.google.common.primitives.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MessageHandlerSupport implements MessageHandler {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MessageHandlerSupport.class);

    private final ConfigurationSupport<Command, CommandConfigurable<?>, Integer> commandConfiguration;
    private final ConfigurationSupport<Function, FunctionConfigurable, Integer> functionConfiguration;

    protected MessageHandlerSupport(ConfigurationSupport<Command, CommandConfigurable<?>, Integer> commandConfiguration, ConfigurationSupport<Function, FunctionConfigurable, Integer> functionConfiguration) {
        this.commandConfiguration = commandConfiguration;
        this.functionConfiguration = functionConfiguration;
    }

    protected byte[] addCheckSum(byte[] messageBytes) {
        // ChkSm: Command Number + Command Parameters + Length + STX
        byte checkSumByte = 0;
        for (byte messageByte : messageBytes) {
            checkSumByte += messageByte;
        }
        messageBytes = Bytes.concat(messageBytes, new byte[]{checkSumByte});
        return messageBytes;
    }

    @Override
    public int getStxValue() {
        return 2;
    }

    @Override
    public String getOutputLogHeaderName(int index) {
        return "Output";
    }

    @Override
    public int getAcknowledgeValue() {
        return 10;
    }

    @Override
    public MessageSupport parse(ClientConfigSpec config, byte[] message) {
        int length = message[1];
        int command = message[2];

        byte[] payload = new byte[length - 3];
        System.arraycopy(message, 3, payload, 0, length - 3);

        byte checksum = message[length];

        byte sum = 0;
        for (int i = 0; i < message.length - 1; i++) {
            sum += message[i];
        }

        if (sum != checksum) {
            throw new IllegalArgumentException("Checksum not correct. Received '" + ByteUtilities.bytesToHex(checksum) + "' but expected '" + ByteUtilities.bytesToHex(sum) + "'");
        }

        return this.getCommandConfig(this.getCommand(command)).parse(config, this, message, payload);
    }

    @Override
    public CommandConfigurable getCommandConfig(Command command) {
        return this.getCommandConfiguration().getConfigurable(command);
    }

    @Override
    public FunctionConfigurable getFunctionConfig(Function function) {
        return this.getFunctionConfiguration().getConfigurable(function);
    }

    public ConfigurationSupport<Command, CommandConfigurable<?>, Integer> getCommandConfiguration() {
        return this.commandConfiguration;
    }

    public ConfigurationSupport<Function, FunctionConfigurable, Integer> getFunctionConfiguration() {
        return this.functionConfiguration;
    }

    @Override
    public Function getFunction(int function) {
        return this.getFunctionConfiguration().getConfigObject(function);
    }

    @Override
    public Command getCommand(int command) {
        return this.getCommandConfiguration().getConfigObject(command);
    }

    @Override
    public boolean knows(Command command) {
        return this.getCommandConfiguration().knows(command);
    }

    @Override
    public boolean knows(Function function) {
        return this.getFunctionConfiguration().knows(function);
    }

    protected void setLengthAndCheckSum(byte[] rawBytes) {
        rawBytes[1] = (byte) (rawBytes.length - 1);

        byte checksum = 0;
        for (byte rawByte : rawBytes) {
            checksum += rawByte;
        }

        rawBytes[rawBytes.length - 1] = checksum;
    }

    protected byte[] getStateBytes(ClientConfigSpec config, Function function, OutputState outputState) {
        int number = outputState.getNumber();
        FunctionConfigurable functionConfig = this.getFunctionConfig(function);
        ComponentSpec component = config.getComponent(function, number);
        return functionConfig.getStateCalculator().convertSet(component, outputState.getState());
    }

    protected String parseState(byte[] message, int counter, ClientConfigSpec config, Function function, int number) {
        return ConfigurationSupport.getState(this, config, function, number, message, counter);
    }

    @Override
    public int getLogStateByte(String state) {
        return LogState.valueOf(state.toUpperCase()).getByteValue();
    }
}
