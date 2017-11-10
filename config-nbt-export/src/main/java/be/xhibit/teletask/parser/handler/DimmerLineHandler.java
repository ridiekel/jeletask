package be.xhibit.teletask.parser.handler;

import be.xhibit.teletask.parser.Consumer;

import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DimmerLineHandler extends LineHandlerSupport {
    private static final DimmerLineHandler INSTANCE = new DimmerLineHandler();

    private static final Pattern START_PATTERN = Pattern.compile("\\s*DIMMERS");

    private static final Pattern DIMMER_PATTERN = Pattern.compile("(\\d*)\\s*([^�]*)�\\s*([^�]*)�\\s*(.*)\\s{2}");

    private DimmerLineHandler() {
    }

    public static DimmerLineHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public Pattern getStartPattern() {
        return START_PATTERN;
    }

    @Override
    protected void handle(String startLine, Consumer consumer, ListIterator<String> iterator, String line, int counter) {
        Matcher matcher = DIMMER_PATTERN.matcher(line);
        if (matcher.find()) {
            consumer.dimmer(matcher.group(1), matcher.group(2).trim(), matcher.group(3).trim(), matcher.group(4).trim());
        }
    }
}
