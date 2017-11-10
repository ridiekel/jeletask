package be.xhibit.teletask.parser.handler;

import be.xhibit.teletask.parser.Consumer;

import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RelayLineHandler extends LineHandlerSupport {
    private static final RelayLineHandler INSTANCE = new RelayLineHandler();

    private static final Pattern START_PATTERN = Pattern.compile("\\s*RELAYS");

    private static final Pattern RELAY_PATTERN = Pattern.compile("(\\d*)\\s*([^�]*)�\\s*([^�]*)�\\s*(.*)\\s{2}");

    private RelayLineHandler() {
    }

    public static RelayLineHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public Pattern getStartPattern() {
        return START_PATTERN;
    }

    @Override
    protected void handle(String startLine, Consumer consumer, ListIterator<String> iterator, String line, int counter) {
        Matcher matcher = RELAY_PATTERN.matcher(line);
        if (matcher.find()) {
            consumer.relay(matcher.group(1), matcher.group(2).trim(), matcher.group(3).trim(), matcher.group(4).trim());
        }
    }
}
