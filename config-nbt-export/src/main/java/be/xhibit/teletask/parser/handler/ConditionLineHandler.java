package be.xhibit.teletask.parser.handler;

import be.xhibit.teletask.parser.Consumer;

import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionLineHandler extends LineHandlerSupport {
    private static final ConditionLineHandler INSTANCE = new ConditionLineHandler();

    private static final Pattern START_PATTERN = Pattern.compile("\\s*CONDITIONS");

    private static final Pattern COND_PATTERN = Pattern.compile("^\\s*CON\\s*(\\d*)\\s*([^=]*)$");

    private ConditionLineHandler() {
    }

    public static ConditionLineHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public Pattern getStartPattern() {
        return START_PATTERN;
    }

    @Override
    protected void handle(String startLine, Consumer consumer, ListIterator<String> iterator, String line, int counter) {
        Matcher matcher = COND_PATTERN.matcher(line);
        if (matcher.find()) {
            consumer.condition(matcher.group(1).trim(), matcher.group(2).trim());
        }
    }
}
