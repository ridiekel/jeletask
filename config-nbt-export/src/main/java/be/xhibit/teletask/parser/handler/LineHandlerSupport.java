package be.xhibit.teletask.parser.handler;

import be.xhibit.teletask.parser.Consumer;

import java.util.ListIterator;
import java.util.regex.Pattern;

public abstract class LineHandlerSupport implements LineHandler {
    private static final Pattern END_PATTERN = Pattern.compile("\\s{20,}TELETASK domotic systems");

    @Override
    public void handle(String startLine, Consumer consumer, ListIterator<String> iterator) {
        String line = null;
        int counter = 0;
        for (; iterator.hasNext() && !END_PATTERN.matcher(line = iterator.next()).matches();) {
            line = line.trim();
            if (!line.isEmpty()) {
                counter++;
                this.handle(startLine, consumer, iterator, line, counter);
            }
        }
        iterator.previous();
    }

    protected abstract void handle(String startLine, Consumer consumer, ListIterator<String> iterator, String line, int counter);
}
