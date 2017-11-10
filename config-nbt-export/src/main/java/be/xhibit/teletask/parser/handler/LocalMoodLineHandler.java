package be.xhibit.teletask.parser.handler;

import be.xhibit.teletask.parser.Consumer;

import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalMoodLineHandler extends LineHandlerSupport {
    private static final LocalMoodLineHandler INSTANCE = new LocalMoodLineHandler();

    private static final Pattern START_PATTERN = Pattern.compile("\\s*LOCAL MOODS");

    private static final Pattern LOCAL_MOOD_PATTERN = Pattern.compile("^\\s*LMD\\s*(\\d*)\\s*([^�]*)�\\s*([^�]*)�\\s*(.*)");

    private LocalMoodLineHandler() {
    }

    public static LocalMoodLineHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public Pattern getStartPattern() {
        return START_PATTERN;
    }

    @Override
    protected void handle(String startLine, Consumer consumer, ListIterator<String> iterator, String line, int counter) {
        //LMD  06  Globaal � Mood � Alles Uit (toggle)
        Matcher matcher = LOCAL_MOOD_PATTERN.matcher(line);
        if (matcher.find()) {
            consumer.localMood(matcher.group(1), matcher.group(2).trim(), matcher.group(3).trim(), matcher.group(4).trim());
        }
    }
}
