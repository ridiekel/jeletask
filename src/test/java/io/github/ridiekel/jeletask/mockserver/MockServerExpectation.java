package io.github.ridiekel.jeletask.mockserver;

import java.util.List;
import java.util.function.Supplier;

public record MockServerExpectation(
        MockServerCommand command,
        List<Supplier<MockServerResponse>> responses
) {

}
