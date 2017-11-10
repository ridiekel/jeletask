package be.xhibit.teletask.parser.handler;

import be.xhibit.teletask.parser.Consumer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputInterfaceLineHandler extends InterfaceLineHandlerSupport {
    private static final Pattern INTERFACE_PATTERN = Pattern.compile("(\\d)\\s*(\\w)([^\\s]*)\\s*(.*)");

    private static final InputInterfaceLineHandler INSTANCE = new InputInterfaceLineHandler();

    private static final Pattern START_PATTERN = Pattern.compile("\\s*I\\s\\-\\sINTERFACES");

    private InputInterfaceLineHandler() {
    }

    public static InputInterfaceLineHandler getInstance() {
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
    protected void handle(Consumer consumer, Matcher matcher) {
        consumer.inputInterface(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
    }
}
