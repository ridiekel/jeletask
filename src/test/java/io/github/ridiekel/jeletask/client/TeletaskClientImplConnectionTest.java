package io.github.ridiekel.jeletask.client;

import ch.qos.logback.classic.Level;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator.ValidOnOffToggle;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;
import io.github.ridiekel.jeletask.mockserver.FakeCentral;
import io.github.ridiekel.jeletask.mockserver.LogCapture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.ridiekel.jeletask.client.TestCentralUnit.*;
import static org.assertj.core.api.Assertions.assertThat;
import static io.github.ridiekel.jeletask.client.ClientThreads.eventually;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Connecting, losing the connection and stopping. The client waits a second before every connection attempt and two
 * seconds for an acknowledge, so most of these take that long.
 */
@Timeout(40)
class TeletaskClientImplConnectionTest {
    private static final Set<Function> LOGGED_FUNCTIONS = Set.of(Function.RELAY, Function.LOCMOOD, Function.GENMOOD, Function.MOTOR, Function.DIMMER, Function.COND, Function.SENSOR, Function.FLAG, Function.TIMEDFNC, Function.INPUT);

    private final List<TeletaskClientImpl> clients = new ArrayList<>();
    private FakeCentral central;
    private ClientThreads threads;
    private LogCapture quiet;

    @BeforeEach
    void setUp() {
        this.quiet = LogCapture.quietClient();
        this.central = new FakeCentral();
        this.threads = new ClientThreads();
    }

    @AfterEach
    void tearDown() {
        this.clients.forEach(TeletaskClientImpl::disconnect);
        this.threads.awaitAllGone();
        this.central.close();
        this.quiet.close();
    }

    @Test
    void reconnectsAndAsksEverythingAgainAfterLosingTheConnection() {
        this.central.goOnline();
        TestCentralUnit unit = TestCentralUnit.withKeepAliveEvery(this.central.port(), 200);
        TeletaskClientImpl client = client(unit);
        RecordingListener listener = new RecordingListener();
        client.registerStateChangeListener(listener);
        client.start();
        assertTrue(client.isConnected());
        awaitLogMessagesOn(0);

        this.central.dropConnections();

        eventually().until(() -> !client.isConnected());
        eventually().until(() -> client.isConnected() && this.central.connectionCount() == 2);
        awaitLogMessagesOn(1);
        eventually().until(() -> groupGetFunctionsOn(1).containsAll(Set.of(Function.RELAY, Function.DIMMER)));
        assertEquals(Command.KEEP_ALIVE, this.central.frames(1).getFirst().command());

        // The client really listens on the new connection
        OnOffState on = new OnOffState(ValidOnOffToggle.ON);
        this.central.send(unit.event(Function.RELAY, RELAY_1, on));
        eventually().until(() -> on.equals(unit.getComponent(Function.RELAY, RELAY_1).getState()));
        assertThat(listener.received()).contains(unit.getComponent(Function.RELAY, RELAY_1));
    }

    @Test
    void startKeepsTryingUntilTheCentralUnitComesOnline() {
        TeletaskClientImpl client = client(TestCentralUnit.on(this.central.port()));

        CompletableFuture<Void> start = CompletableFuture.runAsync(client::start);

        // Longer than the wait before the first attempt, so that one was refused
        eventually().during(1200, TimeUnit.MILLISECONDS).until(() -> !client.isConnected() && !start.isDone());

        this.central.goOnline();

        eventually().until(client::isConnected);
        assertFalse(start.isCompletedExceptionally());
        assertEquals(Command.KEEP_ALIVE, this.central.frames(0).getFirst().command());
        awaitLogMessagesOn(0);
    }

    @Test
    void aFailedAttemptClosesItsConnectionBeforeTheNextOne() {
        this.central.acknowledging(false);
        this.central.goOnline();
        TeletaskClientImpl client = client(TestCentralUnit.on(this.central.port()));

        CompletableFuture.runAsync(client::start);

        eventually().until(() -> this.central.closedByClient(0));
        this.central.acknowledging(true);
        eventually().until(client::isConnected);
        assertFalse(this.central.closedByClient(this.central.connectionCount() - 1));
    }

    @Test
    void aCallDuringAReconnectWaitsForItInsteadOfStartingASecondConnection() {
        this.central.goOnline();
        TeletaskClientImpl client = client(TestCentralUnit.withoutKeepAlive(this.central.port()));
        client.start();
        awaitLogMessagesOn(0);
        this.central.goOffline();

        CompletableFuture<Void> restart = CompletableFuture.runAsync(client::restart);
        eventually().until(() -> !client.isConnected());
        CompletableFuture<Void> call = CompletableFuture.runAsync(client::groupGet);
        eventually().during(1200, TimeUnit.MILLISECONDS).until(() -> !call.isDone());
        this.central.goOnline();

        eventually().until(() -> restart.isDone() && call.isDone());
        assertFalse(restart.isCompletedExceptionally());
        assertFalse(call.isCompletedExceptionally());
        eventually().until(() -> groupGetFunctionsOn(1).contains(Function.RELAY));
        assertEquals(2, this.central.connectionCount());
        assertThat(this.threads.started()).extracting(Thread::getName).containsOnlyOnce("keep-alive", "event-listener");
    }

    @Test
    void aCallAfterAFailedStartFailsRightAwayInsteadOfWaitingForTheStart() {
        TeletaskClientImpl client = client(TestCentralUnit.on(this.central.port()));
        client.disconnect();
        assertThrows(IllegalStateException.class, client::start);

        // Would otherwise wait 30 seconds for a start that is no longer running
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> assertThrows(IllegalStateException.class, client::groupGet));
    }

    @Test
    void disconnectWhileConnectingAbortsTheStart() {
        TeletaskClientImpl client = client(TestCentralUnit.on(this.central.port()));
        CompletableFuture<Void> start = CompletableFuture.runAsync(client::start);

        client.disconnect();

        ExecutionException failure = assertThrows(ExecutionException.class, () -> start.get(5, TimeUnit.SECONDS));
        assertInstanceOf(IllegalStateException.class, failure.getCause());
        assertEquals("Stopped while connecting to the central unit", failure.getCause().getMessage());
        assertFalse(client.isConnected());
        assertThat(this.threads.started()).extracting(Thread::getName).doesNotContain("keep-alive", "event-listener");
    }

    @Test
    void stopClosesTheSocketTheStreamsAndTheTimersEvenWhenAListenerFails() {
        this.central.goOnline();
        TeletaskClientImpl client = client(TestCentralUnit.on(this.central.port()));
        RecordingListener listener = new RecordingListener();
        listener.failOnEverything();
        client.registerStateChangeListener(listener);
        client.start();
        // A native image lists a timer thread only shortly after it started
        eventually().untilAsserted(() -> assertThat(this.threads.started()).extracting(Thread::getName).contains("keep-alive", "event-listener", "io"));

        client.stop();

        assertFalse(client.isConnected());
        assertNull(client.getInputStream());
        assertNull(client.getOutputStream());
        assertEquals(1, listener.stops());
        eventually().until(() -> this.central.closedByClient(0));
        this.threads.awaitAllGone();
    }

    @Test
    void disconnectClosesTheSocketTheStreamsAndTheTimers() {
        this.central.goOnline();
        TeletaskClientImpl client = client(TestCentralUnit.on(this.central.port()));
        client.start();

        client.disconnect();

        assertFalse(client.isConnected());
        assertNull(client.getInputStream());
        assertNull(client.getOutputStream());
        eventually().until(() -> this.central.closedByClient(0));
        this.threads.awaitAllGone();
    }

    @Test
    void stoppingAClientThatNeverStartedDoesNothing() {
        TeletaskClientImpl stopped = client(TestCentralUnit.on(this.central.port()));
        TeletaskClientImpl disconnected = client(TestCentralUnit.on(this.central.port()));
        RecordingListener listener = new RecordingListener();
        stopped.registerStateChangeListener(listener);

        stopped.stop();
        disconnected.disconnect();

        assertFalse(stopped.isConnected());
        assertFalse(disconnected.isConnected());
        assertEquals(1, listener.stops());
    }

    @Test
    void aCallOnAClientThatIsNotStartedStartsIt() {
        this.central.goOnline();
        TeletaskClientImpl client = client(TestCentralUnit.on(this.central.port()));

        client.groupGet();

        assertTrue(client.isConnected());
        eventually().until(() -> groupGetFunctionsOn(0).containsAll(Set.of(Function.RELAY, Function.DIMMER)));
    }

    @Test
    void aFailingWriteIsReportedAsCommunicationException() throws IOException {
        this.central.goOnline();
        TeletaskClientImpl client = client(TestCentralUnit.withoutKeepAlive(this.central.port()));
        client.start();
        client.getOutputStream().close();

        TeletaskClientImpl.CommunicationException failure = assertThrows(TeletaskClientImpl.CommunicationException.class,
                () -> client.send(new byte[]{1}, bytes -> ""));

        assertInstanceOf(IOException.class, failure.getCause());
    }

    @Test
    void aClientStoppedBetweenTheCheckAndTheSubmitReportsTheFailure() {
        this.central.goOnline();
        TestCentralUnit unit = TestCentralUnit.on(this.central.port());
        TeletaskClientImpl client = client(unit);
        client.start();
        AtomicBoolean armed = new AtomicBoolean(true);
        ComponentSpec stoppingOnFirstUse = new ComponentSpec() {
            @Override
            public Function getFunction() {
                if (armed.getAndSet(false)) {
                    client.disconnect();
                }
                return Function.RELAY;
            }
        };
        stoppingOnFirstUse.setNumber(RELAY_1);
        List<Exception> failures = new CopyOnWriteArrayList<>();
        AtomicBoolean succeeded = new AtomicBoolean();

        try (LogCapture log = LogCapture.of(TeletaskClientImpl.class, Level.DEBUG)) {
            client.get(stoppingOnFirstUse, (f, n, s) -> succeeded.set(true), (f, n, s, e) -> failures.add(e));

            assertThat(failures).singleElement().isInstanceOf(RejectedExecutionException.class);
            assertThat(log.messages(Level.DEBUG)).anySatisfy(line -> assertThat(line)
                    .startsWith("Exception (java.util.concurrent.RejectedExecutionException) caught in execute: Task"));
        }
        assertFalse(succeeded.get());
    }

    @Test
    void anInterruptedEventListenerKeepsTheInterrupt() {
        this.central.goOnline();
        TeletaskClientImpl client = client(TestCentralUnit.withoutKeepAlive(this.central.port()));
        client.start();
        awaitLogMessagesOn(0);
        TeletaskClientImpl.EventMessageListener listener = client.new EventMessageListener();

        Thread.currentThread().interrupt();
        listener.run();

        assertTrue(Thread.interrupted());
    }

    @Test
    void stopKeepsTheInterruptOfTheCallerAndStillClosesEverything() {
        this.central.goOnline();
        TeletaskClientImpl client = client(TestCentralUnit.withoutKeepAlive(this.central.port()));
        client.start();
        awaitLogMessagesOn(0);
        // Keeps the io thread busy, so that stop really has to wait for it
        this.central.acknowledging(false);
        client.get(Function.RELAY, RELAY_1, (f, n, s) -> {
        }, (f, n, s, e) -> {
        });
        eventually().until(() -> !this.central.frames(Command.GET).isEmpty());

        Thread.currentThread().interrupt();
        client.stop();

        assertTrue(Thread.interrupted());
        assertNull(client.getOutputStream());
        eventually().until(() -> this.central.closedByClient(0));
    }

    @Test
    void aBusyIoThreadDoesNotPileUpEventListenersInFrontOfTheNextCommand() {
        this.central.goOnline();
        TeletaskClientImpl client = client(TestCentralUnit.withoutKeepAlive(this.central.port()));
        client.start();
        awaitLogMessagesOn(0);
        this.central.acknowledging(false);
        client.get(Function.RELAY, RELAY_1, (f, n, s) -> {
        }, (f, n, s, e) -> {
        });
        eventually().until(() -> !this.central.frames(Command.GET).isEmpty());

        // Almost the whole wait for the missing acknowledge. A listener every 20ms taking 10ms would mean a backlog of a second.
        eventually().during(1900, TimeUnit.MILLISECONDS).until(() -> true);
        this.central.acknowledging(true);
        client.get(Function.RELAY, RELAY_2, (f, n, s) -> {
        }, (f, n, s, e) -> {
        });

        eventually().atMost(800, TimeUnit.MILLISECONDS).until(() -> this.central.frames(Command.GET).size() == 2);
    }

    @Test
    void aSocketThatCannotConnectIsClosed() {
        Socket socket = new Socket();

        assertThrows(IOException.class, () -> TeletaskClientImpl.connectOrClose(socket, new InetSocketAddress("127.0.0.1", this.central.port())));

        assertTrue(socket.isClosed());
    }

    @Test
    void aSocketThatConnectsStaysOpen() throws IOException {
        this.central.goOnline();

        try (Socket socket = TeletaskClientImpl.connectOrClose(new Socket(), new InetSocketAddress("127.0.0.1", this.central.port()))) {
            assertTrue(socket.isConnected());
            assertFalse(socket.isClosed());
        }
    }

    @Test
    void aSocketThatFailsToCloseAfterAFailedConnectKeepsTheConnectFailure() {
        IOException closeFailure = new IOException("close");
        Socket socket = new Socket() {
            @Override
            public void connect(SocketAddress endpoint, int timeout) throws IOException {
                throw new IOException("connect");
            }

            @Override
            public synchronized void close() throws IOException {
                throw closeFailure;
            }
        };

        IOException failure = assertThrows(IOException.class, () -> TeletaskClientImpl.connectOrClose(socket, new InetSocketAddress("127.0.0.1", this.central.port())));

        assertEquals("connect", failure.getMessage());
        assertThat(failure.getSuppressed()).containsExactly(closeFailure);
    }

    @Test
    void aConnectionWithoutStreamsIsClosedAndTheNextAttemptIsUsed() {
        this.central.goOnline();
        List<WrappedSocket> sockets = new CopyOnWriteArrayList<>();
        TeletaskClientImpl client = client(TestCentralUnit.on(this.central.port()), (host, port) -> {
            WrappedSocket socket = new WrappedSocket(TeletaskClientImpl.connectOrClose(new Socket(), new InetSocketAddress(host, port)));
            socket.streamsUnavailable = sockets.isEmpty();
            sockets.add(socket);
            return socket;
        });

        client.start();

        assertTrue(client.isConnected());
        assertThat(sockets).hasSize(2);
        assertTrue(sockets.getFirst().isClosed());
        assertFalse(sockets.getLast().isClosed());
        awaitLogMessagesOn(1);
    }

    @Test
    void streamsAndSocketThatFailToCloseAreLoggedAndStillAllClosed() {
        this.central.goOnline();
        WrappedSocket[] socket = new WrappedSocket[1];
        TeletaskClientImpl client = client(TestCentralUnit.withoutKeepAlive(this.central.port()), (host, port) -> {
            socket[0] = new WrappedSocket(TeletaskClientImpl.connectOrClose(new Socket(), new InetSocketAddress(host, port)));
            socket[0].closeFails = true;
            return socket[0];
        });
        client.start();
        awaitLogMessagesOn(0);

        try (LogCapture log = LogCapture.of(TeletaskClientImpl.class, Level.ERROR)) {
            client.disconnect();

            assertThat(log.messages(Level.ERROR)).containsExactly(
                    "Exception (java.io.IOException) caught in stop: input stream",
                    "Exception (java.io.IOException) caught in stop: output stream",
                    "Exception (java.io.IOException) caught in stop: socket"
            );
        }
        assertNull(client.getInputStream());
        assertNull(client.getOutputStream());
        assertTrue(socket[0].isClosed());
        eventually().until(() -> this.central.closedByClient(0));
    }

    private TeletaskClientImpl client(TestCentralUnit unit) {
        return client(unit, (host, port) -> TeletaskClientImpl.connectOrClose(new Socket(), new InetSocketAddress(host, port)));
    }

    private TeletaskClientImpl client(TestCentralUnit unit, TeletaskClientImpl.SocketFactory socketFactory) {
        TeletaskClientImpl client = new TeletaskClientImpl(unit, socketFactory);
        this.clients.add(client);
        return client;
    }

    /**
     * A connected socket that can refuse its streams, or fail every close after it really closed.
     */
    private static final class WrappedSocket extends Socket {
        private final Socket socket;
        volatile boolean streamsUnavailable;
        volatile boolean closeFails;

        WrappedSocket(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void setKeepAlive(boolean on) throws SocketException {
            this.socket.setKeepAlive(on);
        }

        @Override
        public synchronized void setSoTimeout(int timeout) throws SocketException {
            this.socket.setSoTimeout(timeout);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (this.streamsUnavailable) {
                throw new IOException("no streams");
            }
            return new FilterInputStream(this.socket.getInputStream()) {
                @Override
                public void close() throws IOException {
                    super.close();
                    failCloseIfAsked("input stream");
                }
            };
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            if (this.streamsUnavailable) {
                throw new IOException("no streams");
            }
            return new FilterOutputStream(this.socket.getOutputStream()) {
                @Override
                public void write(byte[] bytes, int offset, int length) throws IOException {
                    this.out.write(bytes, offset, length);
                }

                @Override
                public void close() throws IOException {
                    super.close();
                    failCloseIfAsked("output stream");
                }
            };
        }

        @Override
        public synchronized void close() throws IOException {
            this.socket.close();
            failCloseIfAsked("socket");
        }

        @Override
        public boolean isClosed() {
            return this.socket.isClosed();
        }

        private void failCloseIfAsked(String what) throws IOException {
            if (this.closeFails) {
                throw new IOException(what);
            }
        }
    }

    private void awaitLogMessagesOn(int connection) {
        eventually().until(() -> this.central.frames(connection).stream()
                .filter(frame -> frame.command() == Command.LOG)
                .map(FakeCentral.Frame::function)
                .toList()
                .containsAll(LOGGED_FUNCTIONS));
    }

    private List<Function> groupGetFunctionsOn(int connection) {
        return this.central.frames(connection).stream()
                .filter(frame -> frame.command() == Command.GROUPGET)
                .map(FakeCentral.Frame::function)
                .toList();
    }
}
