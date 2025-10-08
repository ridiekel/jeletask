package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.builder.message.messages.GetMessageSupport;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class GetMessage extends GetMessageSupport {
    public GetMessage(CentralUnit centralUnit, Function function, int number) {
        super(function, centralUnit, number);
    }

    @Override
    protected String toLogLine(String message) {
        return String.format("[%s] - [%s] - [%s] - [%s] - [%s]",
                StringUtils.rightPad(message, 10),
                StringUtils.rightPad(this.getFunction().toString(), 10),
                StringUtils.leftPad(Arrays.stream(this.getNumbers()).mapToObj(String::valueOf).collect(Collectors.joining(", ")), 3),
                StringUtils.leftPad(Arrays.stream(this.getNumbers()).boxed().map(i->this.getCentralUnit().getComponent(getFunction(), i)).map(ComponentSpec::getDescription).collect(Collectors.joining(", ")), 40),
                StringUtils.rightPad(this.getCommand().toString(), 10)
        );
    }
}
