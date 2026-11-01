package io.github.ridiekel.jeletask.client.builder.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ridiekel.jeletask.TeletaskReceiver;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator.ValidOnOffToggle;
import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.EventMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.KeepAliveMessage;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MessageUtilitiesTest {
    private static final int RELAY_WHOSE_NUMBER_EQUALS_ACKNOWLEDGE = 10;
    private static final int OTHER_RELAY = 11;
    private static final byte[] QUIET = new byte[0];

    private final ByteArrayOutputStream logOutput = new ByteArrayOutputStream();
    private final Logger logger = loggerWritingTo(Level.TRACE, logOutput);

    private CentralUnit centralUnit;
    private MessageHandler messageHandler;

    @BeforeEach
    void loadCentralUnit() throws IOException {
        try (InputStream config = getClass().getResourceAsStream("/test-config.json")) {
            centralUnit = new ObjectMapper().readValue(config, CentralUnit.class);
        }
        centralUnit.getAllComponents();
        messageHandler = centralUnit.getMessageHandler();
    }

    @Test
    void messageSplitOverConsecutiveReadsIsReassembled() throws Exception {
        byte[] frame = relayOnEvent(OTHER_RELAY);

        assertRelayEvents(receive(chunks(head(frame, 4), tail(frame, 4))), OTHER_RELAY);
    }

    @Test
    void messageIsReassembledWhenTheRestArrivesAfterQuietPolls() throws Exception {
        byte[] frame = relayOnEvent(OTHER_RELAY);

        assertRelayEvents(receive(chunks(head(frame, 4), QUIET, QUIET, QUIET, tail(frame, 4))), OTHER_RELAY);
    }

    @Test
    void blockEndingOnStxWaitsForTheRestOfTheMessage() throws Exception {
        byte[] first = relayOnEvent(OTHER_RELAY);
        byte[] second = relayOnEvent(RELAY_WHOSE_NUMBER_EQUALS_ACKNOWLEDGE);

        List<MessageSupport> messages = receive(chunks(concat(first, head(second, 1)), tail(second, 1)));

        assertRelayEvents(messages, OTHER_RELAY, RELAY_WHOSE_NUMBER_EQUALS_ACKNOWLEDGE);
    }

    @Test
    void acknowledgeValueInsideASplitMessageIsNotAnAcknowledge() throws Exception {
        byte[] frame = relayOnEvent(RELAY_WHOSE_NUMBER_EQUALS_ACKNOWLEDGE);
        KeepAliveMessage running = new KeepAliveMessage(centralUnit);

        List<MessageSupport> messages = MessageUtilities.receive(logger, receiver(chunks(head(frame, 4), tail(frame, 4))), running);

        assertThat(running.isAcknowledged()).isFalse();
        assertRelayEvents(messages, RELAY_WHOSE_NUMBER_EQUALS_ACKNOWLEDGE);
    }

    @Test
    void messageSplitOverARealSocketIsReassembled() throws Exception {
        byte[] frame = relayOnEvent(OTHER_RELAY);

        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             Socket sender = new Socket(InetAddress.getLoopbackAddress(), server.getLocalPort());
             Socket accepted = server.accept()) {
            OutputStream out = sender.getOutputStream();
            InputStream in = accepted.getInputStream();

            out.write(head(frame, 4));
            out.flush();
            Awaitility.await().until(() -> in.available() == 4);

            CompletableFuture<List<MessageSupport>> received = CompletableFuture.supplyAsync(() -> {
                try {
                    return receive(in);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            Awaitility.await().until(() -> in.available() == 0);

            out.write(tail(frame, 4));
            out.flush();

            assertRelayEvents(received.get(5, TimeUnit.SECONDS), OTHER_RELAY);
        }
    }

    @Test
    @Timeout(value = 3, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void messageThatNeverCompletesIsDroppedAndDoesNotCorruptTheNextCall() throws Exception {
        byte[] complete = relayOnEvent(OTHER_RELAY);
        byte[] abandoned = relayOnEvent(RELAY_WHOSE_NUMBER_EQUALS_ACKNOWLEDGE);
        ChunkedInputStream stream = chunks(concat(complete, head(abandoned, 4)));

        assertRelayEvents(receive(stream), OTHER_RELAY);
        assertThat(logged()).contains("WARN").contains("Dropping incomplete message");

        stream.add(relayOnEvent(OTHER_RELAY));
        assertRelayEvents(receive(stream), OTHER_RELAY);
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void streamThatAlwaysEndsMidMessageStillReturnsWhatWasParsed() throws Exception {
        byte[] frame = relayOnEvent(OTHER_RELAY);

        List<MessageSupport> messages = receive(new EndlessStream(head(frame, 4), concat(tail(frame, 4), head(frame, 4))));

        assertThat(messages).isNotEmpty().allSatisfy(message -> assertThat(((EventMessage) message).getNumber()).isEqualTo(OTHER_RELAY));
        assertThat(logged()).contains("Dropping incomplete message");
    }

    private List<MessageSupport> receive(InputStream stream) throws Exception {
        return MessageUtilities.receive(logger, receiver(stream));
    }

    private TeletaskReceiver receiver(InputStream stream) {
        return new TeletaskReceiver() {
            @Override
            public InputStream getInputStream() {
                return stream;
            }

            @Override
            public CentralUnit getCentralUnit() {
                return centralUnit;
            }
        };
    }

    private byte[] relayOnEvent(int number) {
        return messageHandler.createResponseEventMessageForTesting(centralUnit, Function.RELAY,
                new MessageHandler.OutputState(number, new OnOffState(ValidOnOffToggle.ON))).getRawBytes();
    }

    private static void assertRelayEvents(List<MessageSupport> messages, int... numbers) {
        assertThat(messages).hasSize(numbers.length);
        for (int i = 0; i < numbers.length; i++) {
            assertThat(messages.get(i)).isInstanceOf(EventMessage.class);
            EventMessage event = (EventMessage) messages.get(i);
            assertThat(event.getFunction()).isEqualTo(Function.RELAY);
            assertThat(event.getNumber()).isEqualTo(numbers[i]);
            assertThat(event.getState().getState()).isEqualTo(ValidOnOffToggle.ON);
        }
    }

    private String logged() {
        return logOutput.toString(StandardCharsets.UTF_8);
    }

    private static Logger loggerWritingTo(Level level, ByteArrayOutputStream output) {
        return new SimpleLogger("test", level, false, false, false, false, null, ParameterizedMessageFactory.INSTANCE,
                new PropertiesUtil(new Properties()), new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    private static byte[] head(byte[] bytes, int length) {
        return Arrays.copyOfRange(bytes, 0, length);
    }

    private static byte[] tail(byte[] bytes, int from) {
        return Arrays.copyOfRange(bytes, from, bytes.length);
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static ChunkedInputStream chunks(byte[]... chunks) {
        return new ChunkedInputStream(chunks);
    }

    /**
     * Each poll of {@link #available()} sees the next chunk; an empty chunk is a poll on which nothing has arrived yet.
     */
    private static final class ChunkedInputStream extends InputStream {
        private final Deque<byte[]> chunks = new ArrayDeque<>();

        private ChunkedInputStream(byte[]... chunks) {
            this.chunks.addAll(List.of(chunks));
        }

        void add(byte[] chunk) {
            chunks.add(chunk);
        }

        @Override
        public int available() {
            if (!chunks.isEmpty() && chunks.peek().length == 0) {
                chunks.poll();
                return 0;
            }
            return chunks.isEmpty() ? 0 : chunks.peek().length;
        }

        @Override
        public int read() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read(byte[] target, int offset, int length) {
            System.arraycopy(chunks.poll(), 0, target, offset, length);
            return length;
        }
    }

    /**
     * Delivers the first block once and then repeats the second block on every read, forever.
     */
    private static final class EndlessStream extends InputStream {
        private final byte[] repeated;
        private byte[] next;

        private EndlessStream(byte[] first, byte[] repeated) {
            this.next = first;
            this.repeated = repeated;
        }

        @Override
        public int available() {
            return next.length;
        }

        @Override
        public int read() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read(byte[] target, int offset, int length) {
            System.arraycopy(next, 0, target, offset, length);
            next = repeated;
            return length;
        }
    }
}
