package io.jeletask.parser.handler;

import io.jeletask.parser.Consumer;

import java.util.ListIterator;
import java.util.regex.Pattern;

public interface LineHandler {
    Pattern getStartPattern();

    void handle(String startLine, Consumer consumer, ListIterator<String> iterator);
}
