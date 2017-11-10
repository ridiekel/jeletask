package be.xhibit.teletask.parser.handler;

import be.xhibit.teletask.parser.Consumer;

import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SensorLineHandler extends LineHandlerSupport {
    private static final SensorLineHandler INSTANCE = new SensorLineHandler();

    private static final Pattern START_PATTERN = Pattern.compile("\\s*SENSOR ZONES");

    private static final Pattern SENSOR_PATTERN = Pattern.compile("Sensor zone (\\d*): (.*)");
    private static final Pattern SENSOR_TYPE_PATTERN = Pattern.compile("Type sensor: [^:]*:(.*)");

    private SensorLineHandler() {
    }

    public static SensorLineHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public Pattern getStartPattern() {
        return START_PATTERN;
    }

    @Override
    protected void handle(String startLine, Consumer consumer, ListIterator<String> iterator, String line, int counter) {
        Matcher matcher = SENSOR_PATTERN.matcher(line);
        Matcher typeMatcher = SENSOR_TYPE_PATTERN.matcher(iterator.next());
        if (matcher.find() && typeMatcher.find()) {
            consumer.sensor(matcher.group(1), typeMatcher.group(1).trim(), matcher.group(2).trim());
        }
    }
}
