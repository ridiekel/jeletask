package io.github.ridiekel.jeletask.parser.handler;

import io.github.ridiekel.jeletask.parser.NbtConsumer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OutputInterfaceLineHandler extends InterfaceLineHandlerSupport {
    private static final Pattern INTERFACE_PATTERN = Pattern.compile("(\\d)\\s*(\\w)([^\\s]*)\\s([^\\s]*)\\s*(.*)");

    private static final OutputInterfaceLineHandler INSTANCE = new OutputInterfaceLineHandler();
    private static final Pattern START_PATTERN = Pattern.compile("\\s*O\\s\\-\\sINTERFACES");

    private OutputInterfaceLineHandler() {
    }

    public static OutputInterfaceLineHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public Pattern getStartPattern() {
        return START_PATTERN;
    }

    @Override
    protected Pattern getInterfacePattern() {
        return INTERFACE_PATTERN;
    }

    @Override
    protected void handle(NbtConsumer consumer, Matcher matcher) {
        consumer.outputInterface(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), matcher.group(5));
    }
}

