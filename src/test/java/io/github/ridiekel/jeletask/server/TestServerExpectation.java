package io.github.ridiekel.jeletask.server;

import java.util.List;
import java.util.function.Supplier;

public record TestServerExpectation(
        TestServerCommand command,
        List<Supplier<TestServerResponse>> responses
) {

}
