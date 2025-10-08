package io.github.ridiekel.jeletask.client.builder.message.messages;

import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class GetMessageSupport extends FunctionBasedMessageSupport {
    private final int[] numbers;

    protected GetMessageSupport(Function function, CentralUnit centralUnit, int... numbers) {
        super(centralUnit, function);
        this.numbers = numbers;
    }

    public int[] getNumbers() {
        return this.numbers;
    }

    @Override
    protected byte[] getPayload() {
        return Bytes.concat(new byte[]{(byte) this.getMessageHandler().getFunctionConfig(this.getFunction()).getNumber()}, this.getMessageHandler().composeOutput(this.getNumbers()));
    }

    @Override
    public Command getCommand() {
        return Command.GET;
    }

    @Override
    protected String[] getPayloadLogInfo() {
        return new String[]{this.formatFunction(this.getFunction()), this.formatOutput(this.getNumbers())};
    }

    @Override
    protected String getId() {
        return "GET " + super.getId() + "(" + Arrays.stream(this.numbers).mapToObj(String::valueOf).collect(Collectors.joining(",")) + ")";
    }

    @Override
    protected String toLogLine(String message) {
        return String.format("[%s] - [%s] - [%s] - [%s]",
                StringUtils.rightPad(message, 10),
                StringUtils.rightPad(this.getFunction().toString(), 10),
                StringUtils.leftPad(Arrays.stream(this.getNumbers()).mapToObj(String::valueOf).collect(Collectors.joining(", ")), 3),
                StringUtils.rightPad(this.getCommand().toString(), 10)
                );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        GetMessageSupport that = (GetMessageSupport) o;

        return new EqualsBuilder().appendSuper(super.equals(o)).append(numbers, that.numbers).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(numbers).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("function", this.getFunction())
                .append("numbers", numbers)
                .toString();
    }
}
