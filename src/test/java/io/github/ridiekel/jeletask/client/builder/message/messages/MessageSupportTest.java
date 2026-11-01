package io.github.ridiekel.jeletask.client.builder.message.messages;

import ch.qos.logback.classic.Level;
import io.github.ridiekel.jeletask.client.ClientThreads;
import io.github.ridiekel.jeletask.client.RecordingListener;
import io.github.ridiekel.jeletask.client.TeletaskClientImpl;
import io.github.ridiekel.jeletask.client.TestCentralUnit;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator.ValidOnOffToggle;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.DisplayMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.EventMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.GetMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.GroupGetMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.KeepAliveMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.LogMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.SetMessage;
import io.github.ridiekel.jeletask.client.builder.message.strategy.KeepAliveStrategy;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.impl.DisplayMessageState;
import io.github.ridiekel.jeletask.client.spec.state.impl.LogState;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;
import io.github.ridiekel.jeletask.mockserver.FakeCentral;
import io.github.ridiekel.jeletask.mockserver.LogCapture;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.ansi.AnsiColor;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(30)
class MessageSupportTest {
    private static final OnOffState ON = new OnOffState(ValidOnOffToggle.ON);
    private static final byte ACKNOWLEDGE = 10;

    private final FakeCentral central = new FakeCentral();
    /**
     * Runs once, in place of the keep-alive of the first connection attempt. That is the only moment a message can run
     * outside the io thread with nothing else reading the same stream, like the event listener does once started.
     */
    private final AtomicReference<java.util.function.Consumer<TeletaskClientImpl>> whileConnecting = new AtomicReference<>();
    private final TestCentralUnit unit = TestCentralUnit.withKeepAlive(this.central.port(), new KeepAliveStrategy() {
        @Override
        public int getIntervalMillis() {
            return 60_000;
        }

        @Override
        public void execute(TeletaskClientImpl client) {
            var probe = MessageSupportTest.this.whileConnecting.getAndSet(null);
            if (probe != null) {
                probe.accept(client);
            }
        }
    });
    private final TeletaskClientImpl client = new TeletaskClientImpl(this.unit);
    private final ClientThreads threads = new ClientThreads();
    private LogCapture quiet;

    @BeforeEach
    void setUp() {
        this.quiet = LogCapture.quietClient();
    }

    @AfterEach
    void tearDown() {
        this.client.disconnect();
        this.threads.awaitAllGone();
        this.central.close();
        this.quiet.close();
    }

    @Test
    void anInvalidMessageIsNotSent() {
        // The client is not connected: sending would fail on its missing output stream
        TestMessage message = new TestMessage(this.unit, Command.KEEP_ALIVE, false);

        try (LogCapture log = LogCapture.of(MessageSupport.class, Level.WARN)) {
            message.execute(this.client);

            assertThat(log.messages(Level.WARN)).singleElement().asString().startsWith("Invalid request");
        }
        assertFalse(message.isAcknowledged());
    }

    @Test
    void aCommandTheHandlerDoesNotKnowIsNotSent() {
        TestMessage message = new TestMessage(this.unit, Command.ACKNOWLEDGE, true);

        try (LogCapture log = LogCapture.of(MessageSupport.class, Level.WARN)) {
            message.execute(this.client);

            assertThat(log.messages(Level.WARN)).singleElement().asString().contains("does not know of command 'ACKNOWLEDGE'");
        }
        assertFalse(message.isAcknowledged());
    }

    @Test
    void aMessageIsComposedSentAndAcknowledged() {
        this.central.goOnline();
        KeepAliveMessage message = new KeepAliveMessage(this.unit);
        this.whileConnecting.set(message::execute);

        this.client.start();

        assertTrue(message.isAcknowledged());
        assertEquals("02040b0112", this.central.frames(Command.KEEP_ALIVE).getFirst().hex());
    }

    @Test
    void anAcknowledgeStillCountsWhenAListenerFailsOnAnEventArrivingBeforeIt() {
        this.central.goOnline();
        this.central.acknowledging(false);
        this.central.respondWith(frame -> Bytes.concat(this.unit.event(Function.RELAY, TestCentralUnit.RELAY_1, ON), new byte[]{ACKNOWLEDGE}));
        RecordingListener listener = new RecordingListener();
        listener.failOnEverything();
        this.client.registerStateChangeListener(listener);
        KeepAliveMessage message = new KeepAliveMessage(this.unit);
        this.whileConnecting.set(message::execute);

        try (LogCapture log = LogCapture.of(MessageSupport.class, Level.ERROR)) {
            this.client.start();

            assertThat(log.messages(Level.ERROR)).anySatisfy(line -> assertThat(line).contains("Listener failure"));
        }

        assertTrue(message.isAcknowledged());
        assertThat(listener.received()).contains(this.unit.getComponent(Function.RELAY, TestCentralUnit.RELAY_1));
    }

    @Test
    void noAcknowledgeWithinTheWaitingTimeIsAFailure() {
        this.central.goOnline();
        // Silent from the start: a late acknowledge of the log messages must not end up with this message
        this.central.acknowledging(false);
        this.client.start();
        KeepAliveMessage message = new KeepAliveMessage(this.unit);

        AcknowledgeException failure = assertThrows(AcknowledgeException.class, () -> message.execute(this.client));

        assertEquals("KEEPALIVE - Did not receive acknowledge from the Teletask Central Unit within 2000 ms", failure.getMessage());
        assertNotNull(failure.getCause());
        assertFalse(message.isAcknowledged());
    }

    @Test
    void traceLogInfoShowsEveryPartOfRealMessages() {
        try (LogCapture log = LogCapture.of(MessageSupport.class, Level.TRACE)) {
            for (MessageSupport message : realMessages()) {
                byte[] bytes = message instanceof EventMessage event ? event.getRawBytes() : this.unit.getMessageHandler().compose(message.getCommand(), message.getPayload());

                String info = message.getLogInfo(bytes);

                assertThat(info).as(message.getClass().getSimpleName())
                        .contains("Command: " + message.getCommand())
                        .contains("Length: " + bytes[1])
                        .contains("= " + bytes[bytes.length - 1])
                        .contains("Raw Bytes: " + Bytes.bytesToHex(bytes))
                        .contains("| STX | Length | Command | ")
                        .contains("ChkSm |");
                String tableContent = info.lines().skip(info.lines().count() - 2).findFirst().orElseThrow();
                assertThat(tableContent).as(message.getClass().getSimpleName()).containsSubsequence(Bytes.bytesToHexList(bytes));
            }
        }
    }

    @Test
    void traceLogInfoNamesParametersAndFallsBackWhenTheyHaveNoName() {
        try (LogCapture log = LogCapture.of(MessageSupport.class, Level.TRACE)) {
            MessageSupport get = new GetMessage(this.unit, Function.RELAY, TestCentralUnit.RELAY_1);
            assertThat(get.getLogInfo(this.unit.getMessageHandler().compose(get.getCommand(), get.getPayload())))
                    .contains("| Central Unit | Fnc | Number | Number | ChkSm |");

            MessageSupport groupGet = new GroupGetMessage(this.unit, Function.RELAY, TestCentralUnit.RELAY_1, TestCentralUnit.RELAY_2);
            assertThat(groupGet.getLogInfo(this.unit.getMessageHandler().compose(groupGet.getCommand(), groupGet.getPayload())))
                    .containsPattern("\\| Number \\| Number \\| Output Part \\d \\| Output Part \\d \\| ChkSm \\|");

            MessageSupport keepAlive = new KeepAliveMessage(this.unit);
            assertThat(keepAlive.getLogInfo(this.unit.getMessageHandler().compose(keepAlive.getCommand(), keepAlive.getPayload())))
                    .contains("| Param 0 | ChkSm |");
        }
    }

    @Test
    void shortLogInfoHoldsCommandAndPayload() {
        try (LogCapture log = LogCapture.of(MessageSupport.class, Level.INFO)) {
            GetMessage get = new GetMessage(this.unit, Function.RELAY, TestCentralUnit.RELAY_1);

            String info = get.getLogInfo(this.unit.getMessageHandler().compose(get.getCommand(), get.getPayload()));

            assertEquals("Command: GET, Payload: Function: RELAY | 1 | 01, Output: 1 | 00 01", info);
        }
    }

    @Test
    void everyRealMessageCanBeWrittenAsLogLine() {
        for (MessageSupport message : realMessages()) {
            assertThat(message.toColoredLogLine(AnsiColor.GREEN, "SENT")).as(message.getClass().getSimpleName()).contains("[TELETASK  ]");
        }
    }

    private List<MessageSupport> realMessages() {
        DisplayMessageState display = new DisplayMessageState();
        display.setAscii(true);
        display.setMessageLine1("Hello");
        CentralUnit unit = this.unit;
        return List.of(
                new KeepAliveMessage(unit),
                new LogMessage(unit, Function.RELAY, new LogState(ValidOnOffToggle.ON)),
                new GetMessage(unit, Function.RELAY, TestCentralUnit.RELAY_1),
                new GroupGetMessage(unit, Function.RELAY, TestCentralUnit.RELAY_1, TestCentralUnit.RELAY_2, TestCentralUnit.RELAY_WITHOUT_STATE),
                new SetMessage(unit, Function.RELAY, TestCentralUnit.RELAY_1, ON),
                new DisplayMessage(unit, Function.DISPLAYMESSAGE, TestCentralUnit.DISPLAY, display),
                unit.getMessageHandler().createResponseEventMessageForTesting(unit, Function.RELAY, new MessageHandler.OutputState(TestCentralUnit.RELAY_1, ON))
        );
    }

    private static final class TestMessage extends MessageSupport {
        private final Command command;
        private final boolean valid;

        private TestMessage(CentralUnit centralUnit, Command command, boolean valid) {
            super(centralUnit);
            this.command = command;
            this.valid = valid;
        }

        @Override
        protected boolean isValid() {
            return this.valid;
        }

        @Override
        protected byte[] getPayload() {
            return new byte[0];
        }

        @Override
        protected Command getCommand() {
            return this.command;
        }

        @Override
        protected String[] getPayloadLogInfo() {
            return new String[0];
        }

        @Override
        protected String toLogLine(String message) {
            return message;
        }

        @Override
        protected String getId() {
            return "TEST";
        }

        @Override
        public String toString() {
            return "TestMessage";
        }
    }
}
