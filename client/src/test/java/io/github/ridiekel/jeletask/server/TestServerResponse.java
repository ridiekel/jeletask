package io.github.ridiekel.jeletask.server;

import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.EventMessage;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;

@FunctionalInterface
public interface TestServerResponse {
    EventMessage create(CentralUnit centralUnit, MessageSupport message);
}
