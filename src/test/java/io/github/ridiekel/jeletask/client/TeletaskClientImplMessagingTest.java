package io.github.ridiekel.jeletask.client;

import ch.qos.logback.classic.Level;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator.ValidOnOffToggle;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Command;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.DisplayMessageState;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;
import io.github.ridiekel.jeletask.mockserver.FakeCentral;
import io.github.ridiekel.jeletask.mockserver.LogCapture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static io.github.ridiekel.jeletask.client.TestCentralUnit.*;
import static org.assertj.core.api.Assertions.assertThat;
import static io.github.ridiekel.jeletask.client.ClientThreads.eventually;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * What a client sends and receives while it stays connected. These share one client, because connecting takes a second.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(30)
class TeletaskClientImplMessagingTest {
    private static final OnOffState ON = new OnOffState(ValidOnOffToggle.ON);
    private static final OnOffState OFF = new OnOffState(ValidOnOffToggle.OFF);

    private final FakeCentral central = new FakeCentral();
    private final RecordingListener listener = new RecordingListener();
    private final Callbacks callbacks = new Callbacks();
    private final ClientThreads threads = new ClientThreads();
    private LogCapture quiet;
    private TestCentralUnit unit;
    private TeletaskClientImpl client;

    @BeforeAll
    void connect() {
        this.quiet = LogCapture.quietClient();
        this.central.goOnline();
        this.unit = TestCentralUnit.on(this.central.port());
        this.client = new TeletaskClientImpl(this.unit);
        this.client.registerStateChangeListener(this.listener);
        this.client.start();
    }

    @AfterAll
    void disconnect() {
        this.client.stop();
        this.threads.awaitAllGone();
        this.central.close();
        this.quiet.close();
    }

    @BeforeEach
    void freshComponents() {
        component(Function.RELAY, RELAY_1).setState(OFF);
        component(Function.RELAY, RELAY_2).setState(OFF);
        component(Function.RELAY, RELAY_WITHOUT_STATE).setState(null);
    }

    @AfterEach
    void reset() {
        awaitIoThreadCaughtUp();
        this.central.noResponses();
        this.central.clearFrames();
        this.listener.clear();
        this.callbacks.clear();
    }

    @Test
    void getAsksTheCentralUnitForTheStateOfTheComponent() {
        this.central.respondWith(frame -> frame.command() == Command.GET ? this.unit.event(Function.RELAY, RELAY_1, ON) : new byte[0]);

        this.client.get(component(Function.RELAY, RELAY_1), this.callbacks::success, this.callbacks::failure);

        awaitFrame(Command.GET);
        assertEquals(checksummed("02070601010001"), this.central.frames(Command.GET).getFirst().hex());
        eventually().until(() -> ON.equals(component(Function.RELAY, RELAY_1).getState()));
        assertThat(this.callbacks.successes).containsExactly("RELAY/1");
        assertThat(this.callbacks.failures).isEmpty();
    }

    @Test
    void getByFunctionAndNumberFindsTheComponent() {
        this.client.get(Function.RELAY, RELAY_2, this.callbacks::success, this.callbacks::failure);

        awaitFrame(Command.GET);
        assertEquals(checksummed("02070601010002"), this.central.frames(Command.GET).getFirst().hex());
        assertThat(this.callbacks.successes).containsExactly("RELAY/2");
    }

    @Test
    void getOfAComponentOutsideTheConfigurationIsRefused() {
        assertThrows(CentralUnit.ComponentNotFoundInConfigException.class,
                () -> this.client.get(Function.RELAY, 99, this.callbacks::success, this.callbacks::failure));
    }

    @Test
    void setSendsTheCommandAndTheEventUpdatesTheState() {
        this.central.respondWith(frame -> frame.command() == Command.SET ? this.unit.event(Function.RELAY, RELAY_1, ON) : new byte[0]);

        this.client.set(Function.RELAY, RELAY_1, ON, this.callbacks::success, this.callbacks::failure);

        awaitFrame(Command.SET);
        assertEquals(checksummed("02080701010001ff"), this.central.frames(Command.SET).getFirst().hex());
        eventually().until(() -> ON.equals(component(Function.RELAY, RELAY_1).getState()));
        assertThat(this.callbacks.successes).containsExactly("RELAY/1");
    }

    @Test
    void setToTheCurrentStateSendsNothing() {
        this.client.set(Function.RELAY, RELAY_1, OFF, this.callbacks::success, this.callbacks::failure);

        awaitIoThreadCaughtUp();
        assertThat(this.central.frames(Command.SET)).isEmpty();
        assertThat(this.callbacks.successes).isEmpty();
    }

    @Test
    void setWithoutStateIsRefusedBeforeAnythingIsSent() {
        ComponentSpec relay = component(Function.RELAY, RELAY_1);

        assertThrows(IllegalArgumentException.class, () -> this.client.set(relay, null, this.callbacks::success, this.callbacks::failure));

        awaitIoThreadCaughtUp();
        assertThat(this.central.frames(Command.SET)).isEmpty();
    }

    @Test
    void setOnAComponentWithoutKnownStateFirstFetchesTheState() {
        this.central.respondWith(frame -> frame.command() == Command.GET ? this.unit.event(Function.RELAY, RELAY_WITHOUT_STATE, OFF) : new byte[0]);

        this.client.set(Function.RELAY, RELAY_WITHOUT_STATE, ON, this.callbacks::success, this.callbacks::failure);

        awaitFrame(Command.SET);
        assertThat(this.central.commands()).containsExactly(Command.GET, Command.SET);
        assertThat(this.callbacks.successes).containsExactly("RELAY/3");
    }

    @Test
    void setSendsNothingWhenTheFetchedStateIsAlreadyTheRequestedOne() {
        this.central.respondWith(frame -> frame.command() == Command.GET ? this.unit.event(Function.RELAY, RELAY_WITHOUT_STATE, ON) : new byte[0]);

        this.client.set(Function.RELAY, RELAY_WITHOUT_STATE, ON, this.callbacks::success, this.callbacks::failure);

        awaitIoThreadCaughtUp();
        assertThat(this.central.frames(Command.GET)).hasSize(1);
        assertThat(this.central.frames(Command.SET)).isEmpty();
        assertThat(this.callbacks.successes).isEmpty();
    }

    @Test
    void displayMessageSendsBusesAddressesAndText() {
        DisplayMessageState state = new DisplayMessageState();
        state.setAscii(true);
        state.setMessageType("message");
        state.setMessageLine1("Hi");
        state.setMessageBeeps(3);

        this.client.displaymessage(DISPLAY, state, this.callbacks::success, this.callbacks::failure);

        awaitFrame(Command.DISPLAYMESSAGE);
        String line1 = HexFormat.of().formatHex(String.format("%-16s", "Hi").getBytes(StandardCharsets.US_ASCII));
        String line2 = HexFormat.of().formatHex(" ".repeat(16).getBytes(StandardCharsets.US_ASCII));
        assertEquals(checksummed("022b0401" + "0102" + "0304" + "01" + "01" + line1 + line2 + "03"), this.central.frames(Command.DISPLAYMESSAGE).getFirst().hex());
        assertThat(this.callbacks.successes).containsExactly("DISPLAYMESSAGE/5");
    }

    @Test
    void displayMessageWithoutStateIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> this.client.displaymessage(DISPLAY, null, this.callbacks::success, this.callbacks::failure));

        assertThat(this.callbacks.successes).isEmpty();
    }

    @Test
    void displayMessageToADisplayWithoutAddressIsRefused() {
        DisplayMessageState state = new DisplayMessageState();

        assertThrows(IllegalArgumentException.class, () -> this.client.displaymessage(DISPLAY_WITHOUT_ADDRESS, state, this.callbacks::success, this.callbacks::failure));

        assertThat(this.callbacks.successes).isEmpty();
    }

    @Test
    void groupGetAsksForEveryConfiguredFunctionWithItsOwnNumbers() {
        this.client.groupGet();

        eventually().until(() -> this.central.frames(Command.GROUPGET).size() >= 2);
        awaitIoThreadCaughtUp();
        // Not SENSOR, which holds just the bridge without a number on the central unit, and not the excluded DISPLAYMESSAGE
        assertThat(this.central.frames(Command.GROUPGET).stream().map(FakeCentral.Frame::hex)).contains(
                checksummed("020b0901010001" + "0002" + "0003"),
                checksummed("02070901020007")
        );
        assertThat(this.central.frames(Command.GROUPGET).stream().map(FakeCentral.Frame::function)).doesNotContain(Function.SENSOR, Function.DISPLAYMESSAGE);
    }

    @Test
    void eventUpdatesTheStateAndNotifiesTheListeners() {
        this.central.send(this.unit.event(Function.RELAY, RELAY_2, ON));

        eventually().until(() -> !this.listener.received().isEmpty());
        ComponentSpec relay = component(Function.RELAY, RELAY_2);
        assertThat(this.listener.received()).containsExactly(relay);
        assertEquals(ON, relay.getState());
    }

    @Test
    void eventForAComponentOutsideTheConfigurationIsIgnoredAndTheNextOneStillArrives() {
        byte[] unknown = this.unit.event(Function.RELAY, RELAY_1, ON);
        unknown[6] = 99;
        unknown[unknown.length - 1] += 99 - RELAY_1;

        this.central.send(unknown);
        this.central.send(this.unit.event(Function.RELAY, RELAY_2, ON));

        eventually().until(() -> !this.listener.received().isEmpty());
        assertThat(this.listener.received()).containsExactly(component(Function.RELAY, RELAY_2));
        assertEquals(OFF, component(Function.RELAY, RELAY_1).getState());
    }

    @Test
    void eventIsLoggedWithOldAndNewStateAtDebugLevel() {
        try (LogCapture log = LogCapture.of(TeletaskClientImpl.class, Level.DEBUG)) {
            this.central.send(this.unit.event(Function.RELAY, RELAY_2, ON));

            eventually().until(() -> !this.listener.received().isEmpty());

            assertThat(log.messages(Level.DEBUG)).anySatisfy(line -> assertThat(line)
                    .contains("EVENT", "RELAY", "Component 2", OFF.toString(), ON.toString()));
        }
    }

    @Test
    void aFailingSuccessCallbackOfGetEndsUpInTheFailureCallback() {
        assertFailureCallbackGetsTheCallbackFailure(component -> this.client.get(component, this::failingSuccess, this.callbacks::failure));
    }

    @Test
    void aFailingSuccessCallbackOfSetEndsUpInTheFailureCallback() {
        assertFailureCallbackGetsTheCallbackFailure(component -> this.client.set(component, ON, this::failingSuccess, this.callbacks::failure));
    }

    @Test
    void aFailingSuccessCallbackOfDisplayMessageEndsUpInTheFailureCallback() {
        this.client.displaymessage(component(Function.DISPLAYMESSAGE, DISPLAY), new DisplayMessageState(), this::failingSuccess, this.callbacks::failure);

        assertThat(this.callbacks.failures).containsExactly("DISPLAYMESSAGE/5: callback failure");
    }

    @Test
    void sendWritesTheBytesAndOnlyBuildsTheLogLineAtTraceLevel() {
        List<String> built = new ArrayList<>();
        byte[] keepAlive = HexFormat.of().parseHex(checksummed("02040b01"));

        this.client.send(keepAlive, bytes -> {
            built.add("info");
            return "line";
        });
        try (LogCapture log = LogCapture.of(TeletaskClientImpl.class, Level.TRACE)) {
            this.client.send(keepAlive, bytes -> {
                built.add("trace");
                return "line";
            });
            assertThat(log.messages(Level.TRACE)).contains("line");
        }

        assertThat(built).containsExactly("trace");
        eventually().until(() -> this.central.frames(Command.KEEP_ALIVE).size() == 2);
    }

    private void assertFailureCallbackGetsTheCallbackFailure(Consumer<ComponentSpec> call) {
        call.accept(component(Function.RELAY, RELAY_1));

        assertThat(this.callbacks.failures).containsExactly("RELAY/1: callback failure");
        assertThat(this.callbacks.successes).isEmpty();
    }

    private void failingSuccess(Function function, int number, State<?> state) {
        throw new IllegalStateException("callback failure");
    }

    private void awaitFrame(Command command) {
        eventually().until(() -> !this.central.frames(command).isEmpty());
    }

    /**
     * The io thread handles everything in order, so once this group get came through, all that was queued before it did too.
     */
    private void awaitIoThreadCaughtUp() {
        int before = this.central.frames(Command.GROUPGET).size();
        this.client.groupGet(Function.DIMMER, DIMMER_1);
        eventually().until(() -> this.central.frames(Command.GROUPGET).size() > before);
    }

    private ComponentSpec component(Function function, int number) {
        return this.unit.getComponent(function, number);
    }

    private static String checksummed(String hexWithoutChecksum) {
        byte sum = 0;
        for (byte b : HexFormat.of().parseHex(hexWithoutChecksum)) {
            sum += b;
        }
        return hexWithoutChecksum + HexFormat.of().toHexDigits(sum);
    }

    private static final class Callbacks {
        final List<String> successes = new CopyOnWriteArrayList<>();
        final List<String> failures = new CopyOnWriteArrayList<>();

        void success(Function function, int number, State<?> state) {
            this.successes.add(function + "/" + number);
        }

        void failure(Function function, int number, State<?> state, Exception e) {
            this.failures.add(function + "/" + number + ": " + e.getMessage());
        }

        void clear() {
            this.successes.clear();
            this.failures.clear();
        }
    }
}
