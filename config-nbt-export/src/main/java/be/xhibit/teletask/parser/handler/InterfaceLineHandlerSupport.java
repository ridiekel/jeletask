package be.xhibit.teletask.parser.handler;

import be.xhibit.teletask.parser.Consumer;

import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class InterfaceLineHandlerSupport extends LineHandlerSupport {
    @Override
    protected final void handle(String startLine, Consumer consumer, ListIterator<String> iterator, String line, int counter) {
        Matcher matcher = this.getInterfacePattern().matcher(line);
        if (matcher.find()) {
            this.handle(consumer, matcher);
        }
    }

    protected abstract Pattern getInterfacePattern();

    protected abstract void handle(Consumer consumer, Matcher matcher);
}
