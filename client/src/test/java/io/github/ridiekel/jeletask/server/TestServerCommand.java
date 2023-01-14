package io.github.ridiekel.jeletask.server;

import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;

public record TestServerCommand(
        MessageSupport command
) {
}
