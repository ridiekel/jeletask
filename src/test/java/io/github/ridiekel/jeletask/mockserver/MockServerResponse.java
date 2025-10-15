package io.github.ridiekel.jeletask.mockserver;

import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.EventMessage;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;

public interface MockServerResponse {
    EventMessage create(CentralUnit centralUnit, MessageSupport message);
}
