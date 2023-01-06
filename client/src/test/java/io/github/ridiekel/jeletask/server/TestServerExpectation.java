package io.github.ridiekel.jeletask.server;

import java.util.List;

public record TestServerExpectation(
        TestServerCommand command,
        List<TestServerResponse> responses
) {

}
