package io.github.ridiekel.jeletask.client.spec.config;

import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The config file has one or more problems that keep the bridge from starting. The message lists all of them.
 */
@Getter
public class InvalidConfigException extends RuntimeException {
    private final transient List<String> errors;

    public InvalidConfigException(String source, List<String> errors) {
        super(message(source, errors));
        this.errors = List.copyOf(errors);
    }

    private static String message(String source, List<String> errors) {
        return errors.stream().map(e -> "  - " + e).collect(Collectors.joining(
                System.lineSeparator(),
                "The config file " + source + " has " + errors.size() + (errors.size() == 1 ? " problem:" : " problems:") + System.lineSeparator(),
                ""));
    }
}
