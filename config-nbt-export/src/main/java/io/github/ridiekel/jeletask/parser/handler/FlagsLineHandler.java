package io.github.ridiekel.jeletask.parser.handler;

import io.github.ridiekel.jeletask.parser.NbtConsumer;

import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlagsLineHandler extends LineHandlerSupport {
    private static final FlagsLineHandler INSTANCE = new FlagsLineHandler();

    private static final Pattern START_PATTERN = Pattern.compile("\\s*FLAGS");

    private static final Pattern FLAG_PATTERN = Pattern.compile("^\\s*FLG\\s*(\\d*)\\s*\\d*\\s*([^�]*)�\\s*([^�]*)�\\s*(.*)");

    private FlagsLineHandler() {
    }

    public static FlagsLineHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public Pattern getStartPattern() {
        return START_PATTERN;
    }

    @Override
    protected void handle(String startLine, NbtConsumer consumer, ListIterator<String> iterator, String line, int counter) {
        //FLG 01  01  Garage � Control � Poort Gesloten
        Matcher matcher = FLAG_PATTERN.matcher(line);
        if (matcher.find()) {
            consumer.flag(matcher.group(1), matcher.group(2).trim(), matcher.group(3).trim(), matcher.group(4).trim());
        }
    }
}
