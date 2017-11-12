package io.github.ridiekel.jeletask.parser.handler;

import io.github.ridiekel.jeletask.parser.NbtConsumer;

import java.util.ListIterator;
import java.util.regex.Pattern;

public interface LineHandler {
    Pattern getStartPattern();

    void handle(String startLine, NbtConsumer consumer, ListIterator<String> iterator);
}
