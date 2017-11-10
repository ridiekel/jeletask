package be.xhibit.teletask.parser.handler;

import be.xhibit.teletask.parser.Consumer;

import java.text.DecimalFormat;
import java.util.ListIterator;
import java.util.regex.Pattern;

public class RoomLineHandler extends LineHandlerSupport {
    private static final DecimalFormat ID_FORMATTER = new DecimalFormat("00");

    private static final RoomLineHandler INSTANCE = new RoomLineHandler();
    private static final Pattern START_PATTERN = Pattern.compile("\\s*ROOMS");

    private RoomLineHandler() {
    }

    public static RoomLineHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public Pattern getStartPattern() {
        return START_PATTERN;
    }

    @Override
    protected void handle(String startLine, Consumer consumer, ListIterator<String> iterator, String line, int counter) {
        consumer.room(ID_FORMATTER.format(counter), line);
    }
}
