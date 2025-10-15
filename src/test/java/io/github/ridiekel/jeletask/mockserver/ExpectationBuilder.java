package io.github.ridiekel.jeletask.mockserver;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.*;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.LogState;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ExpectationBuilder {
    private final CentralUnit centralUnit;
    @Getter
    private final List<MockServerExpectation> mocks = new ArrayList<>();

    public ExpectationBuilder(CentralUnit centralUnit) {
        this.centralUnit = centralUnit;
    }

    public WithBuilder with(Function function, Integer number) {
        return new WithBuilder(centralUnit, mocks, function, number);
    }

    public static java.util.function.Function<CentralUnit, MockServerCommand> set(Function function, int number, State<?> state) {
        return c -> new MockServerCommand(new SetMessage(c, function, number, state));
    }

    public static java.util.function.Function<CentralUnit, MockServerCommand> get(Function function, int number) {
        return c -> new MockServerCommand(new GetMessage(c, function, number));
    }

    public static java.util.function.Function<WhenBuilder, MockServerCommand> groupGet(Function function, int... numbers) {
        return c -> new MockServerCommand(new GroupGetMessage(c.getBuilder().centralUnit, function, numbers));
    }

    public static java.util.function.Function<WhenBuilder, MockServerCommand> log(Function function, OnOffToggleStateCalculator.ValidOnOffToggle state) {
        return c -> new MockServerCommand(new LogMessage(c.getBuilder().centralUnit, function, new LogState(state)));
    }

    public static java.util.function.Function<WhenBuilder, MockServerCommand> keepAlive() {
        return c -> new MockServerCommand(new KeepAliveMessage(c.getBuilder().centralUnit));
    }

    public WhenBuilder when(java.util.function.Function<WhenBuilder, MockServerCommand> command) {
        return new WhenBuilder(this, mocks, command);
    }

    public static class WithBuilder {

        @Getter
        private final CentralUnit centralUnit;
        private final List<MockServerExpectation> mocks;
        @Getter
        private final Function function;
        @Getter
        private final Integer number;

        public WithBuilder(CentralUnit centralUnit, List<MockServerExpectation> mocks, Function function, Integer number) {
            this.centralUnit = centralUnit;
            this.mocks = mocks;
            this.function = function;
            this.number = number;
        }

        public static java.util.function.Function<WithBuilder, MockServerCommand> set(Enum<?> state) {
            return set(TeletaskMockServer.template("state").apply(state.toString()));
        }

        public static java.util.function.Function<WithBuilder, MockServerCommand> set(String state) {
            return b -> new MockServerCommand(new SetMessage(b.getCentralUnit(), b.getFunction(), b.getNumber(), b.centralUnit.stateFromMessage(b.getFunction(), b.getNumber(), state)));
        }

        public static java.util.function.Function<WithBuilder, MockServerCommand> set(State<?> state) {
            return b -> new MockServerCommand(new SetMessage(b.getCentralUnit(), b.getFunction(), b.getNumber(), state));
        }

        public static java.util.function.Function<WithBuilder, MockServerCommand> get() {
            return b -> new MockServerCommand(new GetMessage(b.getCentralUnit(), b.getFunction(), b.getNumber()));
        }

        public ResponseBuilder when(java.util.function.Function<WithBuilder, MockServerCommand> command) {
            return new ResponseBuilder(this, mocks, command);
        }

        public static class ResponseBuilder {
            private static final Logger LOG = LoggerFactory.getLogger(ResponseBuilder.class);

            private final java.util.function.Function<WithBuilder, MockServerCommand> command;
            private final List<MockServerExpectation> mocks;
            private final WithBuilder builder;

            public ResponseBuilder(WithBuilder builder, List<MockServerExpectation> mocks, java.util.function.Function<WithBuilder, MockServerCommand> command) {
                this.builder = builder;
                this.command = command;
                this.mocks = mocks;
            }

            public void thenRespond(State<?> state) {
                thenRespond(Collections.singletonList(state));
            }

            public static java.util.function.Function<WithBuilder, Supplier<MockServerResponse>> cachedState() {
                return b -> ExpectationBuilder.WithBuilder.ResponseBuilder.state(b.getCentralUnit().getComponent(b.getFunction(), b.getNumber()).getState()).apply(b);
            }

            public static java.util.function.Function<WithBuilder, Supplier<MockServerResponse>> state(State<?> state) {
                return builder -> () -> (centralUnit, message) -> centralUnit.getMessageHandler().createResponseEventMessageForTesting(centralUnit, builder.getFunction(), new MessageHandler.OutputState(builder.getNumber(), state));
            }

            public void thenRespond(List<State<?>> states) {
                log(states.toString());

                thenRespondFunctional(states.stream()
                        .map(ResponseBuilder::state)
                        .toList());
            }

            private void log(String state) {
                MockServerCommand command = this.command.apply(this.builder);

                if (command.command() instanceof SetMessage message) {
                    LOG.info(String.format(AnsiOutput.toString(AnsiColor.BLUE, "[%s] - [%s] - [%s] - mocking requested state [", AnsiColor.GREEN, "%s", AnsiColor.BLUE, "] -> [", AnsiColor.YELLOW, "%s", AnsiColor.BLUE, "]", AnsiColor.DEFAULT),
                            StringUtils.rightPad("MOCK", 10),
                            StringUtils.rightPad(message.getFunction().toString(), 10),
                            StringUtils.leftPad(String.valueOf(message.getNumber()), 3),
                            message.getState(),
                            state
                    ));
                } else {
                    LOG.info(String.format("[%s] - [%s]",
                            StringUtils.rightPad("MOCK", 10),
                            command.command()));
                }
            }

            public void thenRespondFunctional(java.util.function.Supplier<State<?>> state) {
                thenRespondFunctional(Stream.of(state)
                        .map(s -> (java.util.function.Function<WithBuilder, Supplier<MockServerResponse>>) b -> (Supplier<MockServerResponse>) () -> (MockServerResponse) (centralUnit, message) -> centralUnit.getMessageHandler().createResponseEventMessageForTesting(centralUnit, builder.getFunction(), new MessageHandler.OutputState(builder.getNumber(), s.get())))
                        .toList());
            }

            public void thenRespondFunctional(List<java.util.function.Function<WithBuilder, Supplier<MockServerResponse>>> responses) {
                this.mocks.add(new MockServerExpectation(this.command.apply(this.builder), responses.stream()
                        .map(r -> r.apply(this.builder))
                        .toList()));
            }
        }
    }

    public static class WhenBuilder {
        private final java.util.function.Function<WhenBuilder, MockServerCommand> command;
        private final List<MockServerExpectation> mocks;
        @Getter
        private final ExpectationBuilder builder;

        public WhenBuilder(ExpectationBuilder builder, List<MockServerExpectation> mocks, java.util.function.Function<WhenBuilder, MockServerCommand> command) {
            this.builder = builder;
            this.command = command;
            this.mocks = mocks;
        }

        public static MockServerResponse state(Function function, int number, String state) {
            return (centralUnit, message) -> state(function, number, centralUnit.stateFromMessage(function, number, state)).create(centralUnit, message);
        }

        public static MockServerResponse state(Function function, int number, State<?> state) {
            return (centralUnit, message) -> centralUnit.getMessageHandler().createResponseEventMessageForTesting(centralUnit, function, new MessageHandler.OutputState(number, state));
        }

        public void thenRespond(MockServerResponse... responses) {
            thenRespond(Arrays.asList(responses));
        }

        public void thenRespond(List<MockServerResponse> responses) {
            thenRespondBasedOnCurrentState(responses.stream().map(r -> (Supplier<MockServerResponse>) () -> r).toList());
        }

        public void thenRespondBasedOnCurrentState(List<Supplier<MockServerResponse>> responses) {
            this.mocks.add(new MockServerExpectation(this.command.apply(this), responses));
        }
    }
}
