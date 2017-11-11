package io.jeletask.teletask.parser.handler;

import io.jeletask.teletask.parser.Consumer;

import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MotorLineHandler extends LineHandlerSupport {
    private static final MotorLineHandler INSTANCE = new MotorLineHandler();

    private static final Pattern START_PATTERN = Pattern.compile("\\s*MOTORS");

    private static final Pattern MOTOR_PATTERN = Pattern.compile("(\\d*)\\s*([^�]*)�\\s*([^�]*)�\\s*(.*)\\s*.*");

    private MotorLineHandler() {
    }

    public static MotorLineHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public Pattern getStartPattern() {
        return START_PATTERN;
    }

    @Override
    protected void handle(String startLine, Consumer consumer, ListIterator<String> iterator, String line, int counter) {
        Matcher matcher = MOTOR_PATTERN.matcher(line);
        if (matcher.find()) {
            consumer.motor(matcher.group(1), matcher.group(2).trim(), matcher.group(3).trim(), matcher.group(4).trim());
        }
    }
}
