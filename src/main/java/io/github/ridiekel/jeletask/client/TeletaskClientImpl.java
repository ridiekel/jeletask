package io.github.ridiekel.jeletask.client;

import io.github.ridiekel.jeletask.TeletaskReceiver;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.StateCalculator;
import io.github.ridiekel.jeletask.client.builder.message.MessageUtilities;
import io.github.ridiekel.jeletask.client.builder.message.executor.MessageExecutor;
import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.*;
import io.github.ridiekel.jeletask.client.builder.message.strategy.KeepAliveStrategy;
import io.github.ridiekel.jeletask.client.listener.StateChangeListener;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.DisplayMessageState;
import io.github.ridiekel.jeletask.client.spec.state.impl.LogState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.github.ridiekel.jeletask.utilities.Wait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Service
public final class TeletaskClientImpl implements TeletaskReceiver, TeletaskClient {
    private static final Logger LOG = LogManager.getLogger();
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final String EXCEPTION_IN_STOP = "Exception ({}) caught in stop: {}";

    private Socket socket;
    @Getter
    private final CentralUnit centralUnit;
    private InputStream inputStream;
    @Getter
    private OutputStream outputStream;

    private ExecutorService ioService;

    private Timer keepAliveTimer;
    private Timer eventListenerTimer;

    private final List<StateChangeListener> stateChangeListeners = new ArrayList<>();
    private EventMessageListener eventMessageListener;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean starting = new AtomicBoolean(false);
    private final AtomicBoolean stopping = new AtomicBoolean(false);
    // Counts the connections that were made, to tell the current one from an earlier one
    private final AtomicInteger connections = new AtomicInteger();
    private final AtomicBoolean eventListenerQueued = new AtomicBoolean(false);
    private final SocketFactory socketFactory;
    // One round of connecting: a start fails after it, a reconnect begins the next one
    @Setter(AccessLevel.PACKAGE)
    private Duration connectMaxWait = Duration.ofMinutes(10);

    @Autowired
    public TeletaskClientImpl(CentralUnit centralUnit) {
        this(centralUnit, (host, port) -> connectOrClose(new Socket(), new InetSocketAddress(host, port)));
    }

    TeletaskClientImpl(CentralUnit centralUnit, SocketFactory socketFactory) {
        this.centralUnit = centralUnit;
        this.socketFactory = socketFactory;
    }

    @FunctionalInterface
    interface SocketFactory {
        Socket connect(String host, int port) throws IOException;
    }

// ################################################ PUBLIC API FUNCTIONS

    @Override
    public void get(ComponentSpec component, SuccessConsumer onSuccess, FailureConsumer onFailed) {
        withStarted(() -> this.execute(
                new GetMessage(this.getCentralUnit(), component.getFunction(), component.getNumber()),
                m -> onSuccess.execute(component.getFunction(), component.getNumber(), component.getState()),
                (m, e) -> onFailed.execute(component.getFunction(), component.getNumber(), component.getState(), e)
        ));
    }

    @Override
    public void set(ComponentSpec component, State<?> state, SuccessConsumer onSuccess, FailureConsumer onFailed) {
        withStarted(() -> {
            if (Objects.isNull(component.getState())) {
                this.get(component,
                        (f, n, s) -> LOG.info("State for {} / {} was somehow null, we reset the state to: {}", component.getFunction(), component.getNumber(), component.getState()),
                        (f, n, s, e) -> {
                        });
                Wait.until("State update", Duration.ofSeconds(5), Duration.ofMillis(10), () -> !Objects.isNull(component.getState()));
            }

            if (!Objects.equals(component.getState(), state)) {
                StateCalculator<State<?>> stateCalculator = this.getCentralUnit().getMessageHandler().getFunctionConfig(component.getFunction()).getStateCalculator(component);
                List<SetMessage> messages = stateCalculator.toCommandStates(component, Optional.ofNullable(state).orElseThrow(() -> new IllegalArgumentException("State should not be null"))).stream()
                        .map(s -> new SetMessage(this.getCentralUnit(), component.getFunction(), component.getNumber(), s))
                        .toList();
                // Compose every command up front: an invalid state is rejected here, before anything is sent
                messages.forEach(SetMessage::getStateBytes);
                messages.forEach(message -> this.execute(
                        message,
                        m -> onSuccess.execute(component.getFunction(), component.getNumber(), component.getState()),
                        (m, e) -> onFailed.execute(component.getFunction(), component.getNumber(), component.getState(), e)
                ));
            }
        });
    }

    @Override
    public void displaymessage(ComponentSpec component, DisplayMessageState state, SuccessConsumer onSuccess, FailureConsumer onFailed) {
        withStarted(() -> this.execute(
                new DisplayMessage(this.getCentralUnit(), component.getFunction(), component.getNumber(), Optional.ofNullable(state).orElseThrow(() -> new IllegalArgumentException("State should not be null"))),
                m -> onSuccess.execute(component.getFunction(), component.getNumber(), component.getState()),
                (m, e) -> onFailed.execute(component.getFunction(), component.getNumber(), component.getState(), e)
        ));
    }

    @Override
    public void groupGet() {
        withStarted(() -> {
            for (Function function : Function.values()) {
                if (function.isIncludeInGroupGet()) {
                    this.groupGet(function);
                }
            }
        });
    }

    private void withStarted(Runnable runnable) {
        if (!started.get()) {
            if (!starting.get()) {
                this.start();
            }

            Wait.until("Teletask Client Startup", Duration.ofSeconds(30), Duration.ofMillis(100), started::get);
        }

        runnable.run();
    }

    @Override
    public void registerStateChangeListener(StateChangeListener listener) {
        this.stateChangeListeners.add(listener);
    }

    @Override
    public void set(Function function, int number, State<?> state, SuccessConsumer onSucccess, FailureConsumer onFailed) {
        this.set(this.getComponent(function, number), state, onSucccess, onFailed);
    }

    @Override
    public void displaymessage(int number, DisplayMessageState state, SuccessConsumer onSucccess, FailureConsumer onFailed) {
        this.displaymessage(this.getComponent(Function.DISPLAYMESSAGE, number), state, onSucccess, onFailed);
    }

    @Override
    public void get(Function function, int number, SuccessConsumer onSucccess, FailureConsumer onFailed) {
        this.get(this.getComponent(function, number), onSucccess, onFailed);
    }

    public void start() {
        this.starting.set(true);
        this.started.set(false);

        try {
            LOG.info("Starting IO service...");
            this.startIoService();

            LOG.info("Connecting to central unit...");
            connectAndWait();

            LOG.info("Starting event listener...");
            this.startEventListener();

            LOG.info("Performing group get...");
            this.askForStateChanges(() -> this.sendLogEventMessages(new LogState(OnOffToggleStateCalculator.ValidOnOffToggle.ON)));

            LOG.info("Starting keepalive...");
            this.startKeepAlive();

            this.started.set(true);
        } finally {
            this.starting.set(false);
        }
    }

    private void connectAndWait() {
        String host = this.getCentralUnit().getHost();
        int port = this.getCentralUnit().getPort();

        // A second before every attempt, also the first: a central unit that just dropped us is not hammered right away
        Wait.until("Connect", this.connectMaxWait, Duration.ofSeconds(1), Duration.ofSeconds(1), () -> this.stopping.get() || this.connect(host, port));

        if (this.stopping.get()) {
            throw new IllegalStateException("Stopped while connecting to the central unit");
        }
    }

    /**
     * A reconnect only ends when the client is stopped. Nothing else would start the client again once it gave up:
     * the keep-alive only runs on a started client, and commands wait for the client without starting it.
     */
    private void connectAndKeepTrying() {
        boolean connected = false;
        while (!connected) {
            try {
                this.connectAndWait();
                connected = true;
            } catch (IllegalStateException e) {
                if (this.stopping.get() || Thread.currentThread().isInterrupted()) {
                    throw e;
                }
                LOG.warn("The central unit has been out of reach for {} minute(s), still trying to connect", this.connectMaxWait.toMinutes());
            }
        }
    }

    /**
     * A central unit only reports the changes of a function after the log message for it. When one of those fails on
     * a connection that stays up, for instance on a line that hiccups right after the reconnect, the keep-alive keeps
     * finding that connection fine and nobody would notice that the bridge went deaf. So such a connection is
     * replaced by a new one.
     */
    private void askForStateChanges(Runnable messages) {
        int connection = this.connections.get();
        this.getIoService().execute(() -> {
            try {
                messages.run();
            } catch (RuntimeException e) {
                if (this.stopping.get() || this.starting.get() || this.connections.get() != connection) {
                    // Stopping, or someone else is replacing that connection or did so already
                    LOG.debug("Not reconnecting after failing to ask the central unit for its state changes: {}", e.getMessage());
                    return;
                }
                LOG.warn("Could not ask the central unit for its state changes, reconnecting: {}", e.getMessage());
                this.restart();
            }
        });
    }

    public void restart() {
        // Also counts as starting: a call in the meantime has to wait for this reconnect, not begin a second one
        this.starting.set(true);
        this.started.set(false);

        try {
            this.closeInputStream();
            this.closeOutputStream();
            this.closeSocket();

            this.connectAndKeepTrying();

            this.askForStateChanges(() -> {
                this.groupGet();

                this.sendLogEventMessages(new LogState(OnOffToggleStateCalculator.ValidOnOffToggle.ON));
            });

            this.started.set(true);
        } finally {
            this.starting.set(false);
        }
    }

    @Override
    public boolean isConnected() {
        return this.started.get();
    }

    @Override
    public void disconnect() {
        this.stopping.set(true);
        this.started.set(false);
        // Parts are null when the client never connected, e.g. when the application stops during startup
        if (this.keepAliveTimer != null) {
            this.keepAliveTimer.cancel();
        }
        if (this.eventListenerTimer != null) {
            this.eventListenerTimer.cancel();
        }
        if (this.ioService != null) {
            this.ioService.shutdown();
        }
        this.closeInputStream();
        this.closeOutputStream();
        this.closeSocket();
    }

    public void stop() {
        this.started.set(false);

        Collection<Runnable> runnables = new ArrayList<>();

        runnables.add(this::stopKeepAliveService);
        runnables.add(this::stopEventListener);
        runnables.add(this::stopStateChangeListeners);
        runnables.add(this::stopIoService);
        runnables.add(this::closeInputStream);
        runnables.add(this::closeOutputStream);
        runnables.add(this::closeSocket);

        this.runRunnables(runnables);
    }

    public void send(byte[] message, java.util.function.Function<byte[], String> logMessage) {
        if (LOG.isTraceEnabled()) {
            LOG.trace(logMessage.apply(message));
        }
        try {
            MessageUtilities.send(getOutputStream(), message);
        } catch (IOException e) {
            throw new CommunicationException("Problem sending message to teletask central unit", e);
        }
    }

    public static class CommunicationException extends RuntimeException {
        public CommunicationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // ################################################ PRIVATE API FUNCTIONS
    public void groupGet(Function function, int... numbers) {
        new GroupGetTask(function, numbers).run();
    }

    public void groupGet(Function function) {
        List<ComponentSpec> components = this.getCentralUnit().getComponents(function).filter(c -> c.getNumber() >= 0).toList();
        if (!components.isEmpty()) {
            this.groupGet(function, components.stream().mapToInt(ComponentSpec::getNumber).toArray());
        }
    }

    private void sendLogEventMessages(LogState state) {
        this.sendLogEventMessage(Function.RELAY, state);
        this.sendLogEventMessage(Function.LOCMOOD, state);
        this.sendLogEventMessage(Function.GENMOOD, state);
        this.sendLogEventMessage(Function.MOTOR, state);
        this.sendLogEventMessage(Function.DIMMER, state);
        this.sendLogEventMessage(Function.COND, state);
        this.sendLogEventMessage(Function.SENSOR, state);
        this.sendLogEventMessage(Function.FLAG, state);
        this.sendLogEventMessage(Function.TIMEDFNC, state);
        this.sendLogEventMessage(Function.INPUT, state);
    }

    private <M extends MessageSupport> void execute(
            M message,
            Consumer<M> onSuccess,
            BiConsumer<M, Exception> onFailed) {
        try {
            this.getIoService()
                    .execute(new MessageExecutor(message, this));
            onSuccess.accept(message);
        } catch (Exception e) {
            LOG.debug(() -> String.format("Exception (%s) caught in execute: %s", e.getClass().getName(), e.getMessage()));
            onFailed.accept(message, e);
        }
    }

    private void startEventListener() {
        this.setEventMessageListener(new EventMessageListener());
        this.eventListenerTimer = new Timer("event-listener");
        this.eventListenerTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // The io thread can be busy for minutes, e.g. while reconnecting. A queue full of listeners would keep commands waiting afterwards.
                if (TeletaskClientImpl.this.eventListenerQueued.compareAndSet(false, true)) {
                    TeletaskClientImpl.this.ioService.execute(() -> {
                        TeletaskClientImpl.this.eventListenerQueued.set(false);
                        TeletaskClientImpl.this.getEventMessageListener().run();
                    });
                }
            }
        }, 0, 20);
    }

    private boolean connect(String host, int port) {
        boolean connected = false;

        LOG.info(() -> String.format("(Re)connecting to %s:%s", host, port));

        try {
            this.socket = this.socketFactory.connect(host, port);
            this.socket.setKeepAlive(true);
            this.socket.setSoTimeout(2000);
            connected = true;
        } catch (IOException _) {
            LOG.trace("Problem connecting to host: {}:{}", host, port);
        }

        if (connected) {

            try {
                this.outputStream = this.socket.getOutputStream();
                this.inputStream = this.socket.getInputStream();
            } catch (IOException _) {
                connected = false;
                LOG.trace("Couldn't get I/O for the connection to: {}:{}", host, port);
            }
        }

        if (connected) {
            try {
                this.getCentralUnit().getMessageHandler().getKeepAliveStrategy().execute(this);
            } catch (Exception _) {
                connected = false;
                LOG.trace("Could not send keepalive, assuming not yet connected: {}:{}", host, port);
            }
        }

        if (connected) {
            this.connections.incrementAndGet();
            LOG.info(() -> "Successfully (Re)connected");
        } else {
            this.closeInputStream();
            this.closeOutputStream();
            this.closeSocket();
        }

        return connected;
    }

    static Socket connectOrClose(Socket socket, InetSocketAddress address) throws IOException {
        try {
            socket.connect(address, CONNECT_TIMEOUT_MILLIS);
            return socket;
        } catch (IOException | RuntimeException e) {
            try {
                socket.close();
            } catch (IOException closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
    }

    private void startKeepAlive() {
        KeepAliveStrategy keepAliveStrategy = this.getCentralUnit().getMessageHandler().getKeepAliveStrategy();
        this.keepAliveTimer = new Timer("keep-alive");
        this.keepAliveTimer.schedule(new KeepAliveService(keepAliveStrategy), 0, keepAliveStrategy.getIntervalMillis());
    }


    private void sendLogEventMessage(Function function, LogState state) {
        new LogMessage(this.getCentralUnit(), function, state).execute(this);
    }

    private ExecutorService getIoService() {
        return this.ioService;
    }

    @Override
    public InputStream getInputStream() {
        return this.inputStream;
    }

    public class EventMessageListener implements Runnable {
        @Override
        public void run() {
            try {
                TeletaskClientImpl.this.handleReceiveEvents(MessageUtilities.receive(LOG, TeletaskClientImpl.this));
            } catch (InterruptedException _) {
                // The io service is shutting down; keep the interrupt so its thread can stop
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOG.trace(String.format("Exception (%s) caught in EventMessageListener: %s", e.getClass().getName(), e.getMessage()), e);
            }
        }
    }

    public void handleReceiveEvents(Iterable<MessageSupport> messages) {
        List<ComponentSpec> components = new ArrayList<>();
        for (MessageSupport message : messages) {
            if (message instanceof EventMessage eventMessage) {
                this.handleReceiveEvent(this.getCentralUnit(), eventMessage);
                components.add(this.getComponent(eventMessage.getFunction(), eventMessage.getNumber()));
            }
        }

        if (!components.isEmpty()) {
            for (StateChangeListener stateChangeListener : this.getStateChangeListeners()) {
                stateChangeListener.receive(components);
            }
        }
    }

    private ComponentSpec getComponent(Function function, int number) {
        return this.getCentralUnit().getComponent(function, number);
    }


    private void handleReceiveEvent(CentralUnit centralUnit, EventMessage eventMessage) {
        // Never null: getComponent throws for a component that is not configured, and parsing already drops such events
        ComponentSpec component = centralUnit.getComponent(eventMessage.getFunction(), eventMessage.getNumber());
        LOG.debug(() ->
                String.format(AnsiOutput.toString(AnsiColor.BRIGHT_GREEN, "[TELETASK  ] - [%s] - [%s] - [%s] - [%s] - ", AnsiColor.BRIGHT_CYAN, "%s - ", AnsiColor.BRIGHT_YELLOW, "%s", AnsiColor.BRIGHT_GREEN, " -> ", AnsiColor.BRIGHT_YELLOW, "%s", AnsiColor.DEFAULT),
                        StringUtils.rightPad("EVENT", 10),
                        StringUtils.rightPad(component.getFunction().toString(), 10),
                        StringUtils.leftPad(String.valueOf(component.getNumber()), 3),
                        StringUtils.leftPad(component.getDescription(), 40),
                        Bytes.bytesToHex(eventMessage.getRawBytes()),
                        component.getState(),
                        eventMessage.getState()
                )
        );
        component.setState(eventMessage.getState());
    }

    private void runRunnables(Iterable<Runnable> runnables) {
        runnables.forEach(r -> {
            try {
                r.run();
            } catch (Exception e) {
                LOG.debug(() -> String.format("Exception (%s) caught in stop: %s", e.getClass().getName(), e.getMessage()));
            }
        });
    }

    private List<StateChangeListener> getStateChangeListeners() {
        return this.stateChangeListeners;
    }

    private EventMessageListener getEventMessageListener() {
        return this.eventMessageListener;
    }

    private void setEventMessageListener(EventMessageListener eventMessageListener) {
        this.eventMessageListener = eventMessageListener;
    }

    private void stopIoService() {
        if (this.ioService == null) {
            return;
        }
        try {
            this.getIoService().shutdown();
            this.getIoService().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.debug(() -> String.format("Exception (%s) caught in stop: %s", e.getClass().getName(), e.getMessage()));
            Thread.currentThread().interrupt();
        } finally {
            this.ioService = null;
        }
    }

    private void stopStateChangeListeners() {
        this.getStateChangeListeners().forEach(StateChangeListener::stop);
    }

    private void stopKeepAliveService() {
        if (this.keepAliveTimer == null) {
            return;
        }
        this.keepAliveTimer.cancel();
        this.keepAliveTimer.purge();
        this.keepAliveTimer = null;
    }

    private void stopEventListener() {
        if (this.eventListenerTimer == null) {
            return;
        }
        this.eventListenerTimer.cancel();
        this.eventListenerTimer.purge();
        this.eventListenerTimer = null;
    }

    private void closeSocket() {
        if (this.socket == null) {
            return;
        }
        try {
            this.socket.close();
        } catch (IOException e) {
            LOG.error(EXCEPTION_IN_STOP, e.getClass().getName(), e.getMessage(), e);
        } finally {
            this.socket = null;
        }
    }

    private void closeOutputStream() {
        if (this.outputStream == null) {
            return;
        }
        try {
            this.getOutputStream().flush();
            this.getOutputStream().close();
        } catch (IOException e) {
            LOG.error(EXCEPTION_IN_STOP, e.getClass().getName(), e.getMessage(), e);
        } finally {
            this.outputStream = null;
        }
    }

    private void closeInputStream() {
        if (this.inputStream == null) {
            return;
        }
        try {
            this.getInputStream().close();
        } catch (IOException e) {
            LOG.error(EXCEPTION_IN_STOP, e.getClass().getName(), e.getMessage(), e);
        } finally {
            this.inputStream = null;
        }
    }

    private class KeepAliveService extends TimerTask {
        private final KeepAliveStrategy keepAliveStrategy;

        public KeepAliveService(KeepAliveStrategy keepAliveStrategy) {
            this.keepAliveStrategy = keepAliveStrategy;
        }

        @Override
        public void run() {
            if (started.get()) {
                TeletaskClientImpl.this.getIoService().execute(() -> {
                    try {
                        KeepAliveService.this.keepAliveStrategy.execute(TeletaskClientImpl.this);
                    } catch (Exception e) {
                        LOG.debug(() -> String.format("Exception (%s) caught in KeepAliveService: %s", e.getClass().getName(), e.getMessage()));
                        restart();
                    }
                });
            }
        }
    }

    private class GroupGetTask implements Runnable {
        private final Function function;
        private final int[] numbers;

        public GroupGetTask(Function function, int... numbers) {
            this.function = function;
            this.numbers = numbers;
        }

        @Override
        public void run() {
            getIoService().execute(() -> TeletaskClientImpl.this.getCentralUnit().getMessageHandler().getGroupGetStrategy().execute(TeletaskClientImpl.this, function, numbers));
        }
    }

    private void startIoService() {
        this.ioService = Executors.newSingleThreadExecutor(r -> new Thread(r, "io"));
    }
}
