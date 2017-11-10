package be.xhibit.teletask.parser.handler;

import be.xhibit.teletask.parser.Consumer;

import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeneralMoodLineHandler extends LineHandlerSupport {
    private static final GeneralMoodLineHandler INSTANCE = new GeneralMoodLineHandler();

    private static final Pattern START_PATTERN = Pattern.compile("\\s*GENERAL MOODS");

    private static final Pattern LOCAL_MOOD_PATTERN = Pattern.compile("^\\s*GMD\\s*(\\d*)\\s*([^�]*)�\\s*([^�]*)�\\s*(.*)");

    private GeneralMoodLineHandler() {
    }

    public static GeneralMoodLineHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public Pattern getStartPattern() {
        return START_PATTERN;
    }

    @Override
    protected void handle(String startLine, Consumer consumer, ListIterator<String> iterator, String line, int counter) {
        //GMD  01   �  � Alles Uit gelijkvloers vooraan
        Matcher matcher = LOCAL_MOOD_PATTERN.matcher(line);
        if (matcher.find()) {
            consumer.generalMood(matcher.group(1), matcher.group(2).trim(), matcher.group(3).trim(), matcher.group(4).trim());
        }
    }
}
