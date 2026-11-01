package io.github.ridiekel.jeletask.client;

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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
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
    void disconnectWhileConnectingAbortsTheStart() throws Exception {
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
        assertThat(this.threads.started()).extracting(Thread::getName).contains("keep-alive", "event-listener", "io");

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

        client.get(stoppingOnFirstUse, (f, n, s) -> succeeded.set(true), (f, n, s, e) -> failures.add(e));

        assertThat(failures).singleElement().isInstanceOf(RejectedExecutionException.class);
        assertFalse(succeeded.get());
    }

    @Test
    void aClientCannotBeCloned() {
        TeletaskClientImpl client = client(TestCentralUnit.on(this.central.port()));

        assertThrows(CloneNotSupportedException.class, client::clone);
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

    private TeletaskClientImpl client(TestCentralUnit unit) {
        TeletaskClientImpl client = new TeletaskClientImpl(unit);
        this.clients.add(client);
        return client;
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
