package io.github.ridiekel.jeletask.server;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.GetMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.GroupGetMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.KeepAliveMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.LogMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.SetMessage;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExpectationBuilder {
    private final CentralUnit centralUnit;
    private final List<TestServerExpectation> mocks = new ArrayList<>();

    public ExpectationBuilder(CentralUnit centralUnit) {
        this.centralUnit = centralUnit;
    }

    public WithBuilder with(Function function, Integer number) {
        return new WithBuilder(centralUnit, mocks, function, number);
    }

    public WhenBuilder when(java.util.function.Function<WhenBuilder, TestServerCommand> command) {
        return new WhenBuilder(this, mocks, command);
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

    public static java.util.function.Function<WhenBuilder, TestServerCommand> groupGet(Function function, int... numbers) {
        return c -> new TestServerCommand(new GroupGetMessage(c.getBuilder().centralUnit, function, numbers));
    }
    public static java.util.function.Function<WhenBuilder, TestServerCommand> log(Function function, String state) {
        return c -> new TestServerCommand(new LogMessage(c.getBuilder().centralUnit, function, new ComponentState(state)));
    }

    public static java.util.function.Function<WhenBuilder, TestServerCommand> keepAlive() {
        return c -> new TestServerCommand(new KeepAliveMessage(c.getBuilder().centralUnit));
    }

    public List<TestServerExpectation> getMocks() {
        return this.mocks;
    }

    public static class WithBuilder {

        private final CentralUnit centralUnit;
        private final List<TestServerExpectation> mocks;
        private final Function function;
        private final Integer number;

        public WithBuilder(CentralUnit centralUnit, List<TestServerExpectation> mocks, Function function, Integer number) {
            this.centralUnit = centralUnit;
            this.mocks = mocks;
            this.function = function;
            this.number = number;
        }

        public ResponseBuilder when(java.util.function.Function<WithBuilder, TestServerCommand> command) {
            return new ResponseBuilder(this, mocks, command);
        }

        public static java.util.function.Function<WithBuilder, TestServerCommand> set(String state) {
            return b -> new TestServerCommand(new SetMessage(b.getCentralUnit(), b.getFunction(), b.getNumber(), new ComponentState(state)));
        }

        public static java.util.function.Function<WithBuilder, TestServerCommand> get() {
            return b -> new TestServerCommand(new GetMessage(b.getCentralUnit(), b.getFunction(), b.getNumber()));
        }

        public CentralUnit getCentralUnit() {
            return centralUnit;
        }

        public Function getFunction() {
            return function;
        }

        public Integer getNumber() {
            return number;
        }

        public static class ResponseBuilder {
            private final java.util.function.Function<WithBuilder, TestServerCommand> command;
            private final List<TestServerExpectation> mocks;
            private final WithBuilder builder;

            public ResponseBuilder(WithBuilder builder, List<TestServerExpectation> mocks, java.util.function.Function<WithBuilder, TestServerCommand> command) {
                this.builder = builder;
                this.command = command;
                this.mocks = mocks;
            }

            public void thenRespond(String state) {
                thenRespond(state(state));
            }

            public void thenRespond(ComponentState state) {
                thenRespond(state(state));
            }

            public void thenRespond(java.util.function.Function<WithBuilder, TestServerResponse> response) {
                this.mocks.add(new TestServerExpectation(this.command.apply(this.builder), Collections.singletonList(response.apply(this.builder))));
            }

            public static java.util.function.Function<WithBuilder, TestServerResponse> cachedState() {
                return b -> ExpectationBuilder.WithBuilder.ResponseBuilder.state(b.getCentralUnit().getComponent(b.getFunction(), b.getNumber()).getState()).apply(b);
            }

            public static java.util.function.Function<WithBuilder, TestServerResponse> state(String state) {
                return state(new ComponentState(state));
            }

            public static java.util.function.Function<WithBuilder, TestServerResponse> state(ComponentState state) {
                return b-> (centralUnit, message) -> centralUnit.getMessageHandler().createResponseEventMessage(centralUnit, b.getFunction(), new MessageHandler.OutputState(b.getNumber(), state));
            }
        }
    }

    public static class WhenBuilder {
        private final java.util.function.Function<WhenBuilder, TestServerCommand> command;
        private final List<TestServerExpectation> mocks;
        private final ExpectationBuilder builder;

        public WhenBuilder(ExpectationBuilder builder, List<TestServerExpectation> mocks, java.util.function.Function<WhenBuilder, TestServerCommand> command) {
            this.builder = builder;
            this.command = command;
            this.mocks = mocks;
        }

        public ExpectationBuilder getBuilder() {
            return builder;
        }

        public void thenRespond(TestServerResponse... responses) {
            thenRespond(Arrays.asList(responses));
        }

        public void thenRespond(List<TestServerResponse> responses) {
            this.mocks.add(new TestServerExpectation(this.command.apply(this), responses));
        }

        public static TestServerResponse state(Function function, int number, String state) {
            return state(function, number, new ComponentState(state));
        }

        public static TestServerResponse state(Function function, int number, ComponentState state) {
            return (centralUnit, message) -> centralUnit.getMessageHandler().createResponseEventMessage(centralUnit, function, new MessageHandler.OutputState(number, state));
        }
    }
}
