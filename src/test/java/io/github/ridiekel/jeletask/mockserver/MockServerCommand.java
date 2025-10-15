package io.github.ridiekel.jeletask.mockserver;

import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;

public record MockServerCommand(
        MessageSupport command
) {
}
