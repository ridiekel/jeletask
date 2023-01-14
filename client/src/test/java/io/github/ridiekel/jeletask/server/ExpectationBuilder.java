package io.github.ridiekel.jeletask.server;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.GetMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.GroupGetMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.SetMessage;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.ridiekel.jeletask.server.ExpectationBuilder.ExpectationResponseBuilder.state;

public class ExpectationBuilder {
    private final CentralUnit centralUnit;
    private final List<TestServerExpectation> mocks = new ArrayList<>();

    public ExpectationBuilder(CentralUnit centralUnit) {
        this.centralUnit = centralUnit;
    }

    public ExpectationResponseBuilder when(java.util.function.Function<CentralUnit, TestServerCommand> command) {
        return new ExpectationResponseBuilder(centralUnit, command, mocks);
    }

    public static java.util.function.Function<CentralUnit, TestServerCommand> set(Function function, int number, String state) {
        return set(function, number, new ComponentState(state));
    }

    public static java.util.function.Function<CentralUnit, TestServerCommand> set(Function function, int number, ComponentState state) {
        return c -> new TestServerCommand(new SetMessage(c, function, number, state));
    }

    public static java.util.function.Function<CentralUnit, TestServerCommand> get(Function function, int number) {
        return c -> new TestServerCommand(new GetMessage(c, function, number));
    }

    public static java.util.function.Function<CentralUnit, TestServerCommand> groupGet(Function function, int... numbers) {
        return c -> new TestServerCommand(new GroupGetMessage(c, function, numbers));
    }

    public List<TestServerExpectation> getMocks() {
        return this.mocks;
    }

    public void expect(Function function, int number, String state) {
        this.expect(function, number, state, state);
    }

    public void expect(Function function, int number, String state, String resultingState) {
        when(set(function, number, state)).thenRespond(state(function, number, resultingState));
    }

    public void expect(Function function, int number, ComponentState state) {
        this.expect(function, number, state, state);
    }

    public void expect(Function function, int number, ComponentState state, ComponentState resultingState) {
        when(set(function, number, state)).thenRespond(state(function, number, resultingState));
    }

    public static class ExpectationResponseBuilder {
        private final CentralUnit centralUnit;
        private final java.util.function.Function<CentralUnit, TestServerCommand> command;
        private final List<TestServerExpectation> mocks;

        public ExpectationResponseBuilder(CentralUnit centralUnit, java.util.function.Function<CentralUnit, TestServerCommand> command, List<TestServerExpectation> mocks) {
            this.centralUnit = centralUnit;
            this.command = command;
            this.mocks = mocks;
        }

        public void thenRespond(TestServerResponse... responses) {
            this.mocks.add(new TestServerExpectation(this.command.apply(centralUnit), Arrays.asList(responses)));
        }

        public static TestServerResponse state(Function function, int number, String state) {
            return state(function, number, new ComponentState(state));
        }

        public static TestServerResponse state(Function function, int number, ComponentState state) {
            return (centralUnit, message) -> centralUnit.getMessageHandler().createResponseEventMessage(centralUnit, function, new MessageHandler.OutputState(number, state));
        }
    }
}
