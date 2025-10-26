package io.github.ridiekel.jeletask.client;

import io.github.ridiekel.jeletask.TeletaskReceiver;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
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
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Service
public final class TeletaskClientImpl implements TeletaskReceiver, TeletaskClient {
    private static final Logger LOG = LogManager.getLogger();

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

    @Autowired
    public TeletaskClientImpl(CentralUnit centralUnit) {
        this.centralUnit = centralUnit;
    }

// ################################################ PUBLIC API FUNCTIONS

    @Override
    public void get(ComponentSpec component, SuccessConsumer onSuccess, FailureConsumer onFailed) {
        withStarted(() -> {
            if (this.started.get()) {
                this.execute(
                        new GetMessage(this.getCentralUnit(), component.getFunction(), component.getNumber()),
                        m -> onSuccess.execute(component.getFunction(), component.getNumber(), component.getState()),
                        (m, e) -> onFailed.execute(component.getFunction(), component.getNumber(), component.getState(), e)
                );
            }
        });
    }

    @Override
    public void set(ComponentSpec component, State<?> state, SuccessConsumer onSuccess, FailureConsumer onFailed) {
        withStarted(() -> {
            if (Objects.isNull(component.getState())) {
                this.get(component,
                        (f, n, s) -> LOG.info("State for {} / {} was somehow null, we reset the state to: {}", component.getFunction(), component.getNumber(), component.getState()),
                        (f, n, s, e) -> {
                        });
                Awaitility.await("State update")
                        .atMost(5, TimeUnit.SECONDS)
                        .pollInSameThread()
                        .pollInterval(10, TimeUnit.MILLISECONDS)
                        .until(() -> !Objects.isNull(component.getState()));
            }

            if (!Objects.equals(component.getState(), state)) {
                this.execute(
                        new SetMessage(this.getCentralUnit(), component.getFunction(), component.getNumber(), Optional.ofNullable(state).orElseThrow(() -> new IllegalArgumentException("State should not be null"))),
                        m -> onSuccess.execute(component.getFunction(), component.getNumber(), component.getState()),
                        (m, e) -> onFailed.execute(component.getFunction(), component.getNumber(), component.getState(), e)
                );
            }
        });
    }

    @Override
    public void displaymessage(ComponentSpec component, DisplayMessageState state, SuccessConsumer onSuccess, FailureConsumer onFailed) {
        withStarted(() -> {
            this.execute(
                    new DisplayMessage(this.getCentralUnit(), component.getFunction(), component.getNumber(), Optional.ofNullable(state).orElseThrow(() -> new IllegalArgumentException("State should not be null"))),
                    m -> onSuccess.execute(component.getFunction(), component.getNumber(), component.getState()),
                    (m, e) -> onFailed.execute(component.getFunction(), component.getNumber(), component.getState(), e)
            );
        });
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

            Awaitility.await("Teletask Client Startup").atMost(30, TimeUnit.SECONDS).until(started::get);
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

        LOG.info("Starting IO service...");
        this.startIoService();

        LOG.info("Connecting to central unit...");
        connectAndWait();

        LOG.info("Starting event listener...");
        this.startEventListener();

        LOG.info("Performing group get...");
        this.getIoService().execute(() -> {
            this.sendLogEventMessages(new LogState(OnOffToggleStateCalculator.ValidOnOffToggle.ON));
        });

        LOG.info("Starting keepalive...");
        this.startKeepAlive();

        this.started.set(true);
        this.starting.set(false);
    }

    private void connectAndWait() {
        String host = this.getCentralUnit().getHost();
        int port = this.getCentralUnit().getPort();

        Awaitility.await("Connect")
                .pollInSameThread()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(10, TimeUnit.MINUTES)
                .until(() -> this.connect(host, port));
    }

    public void restart() {
        this.started.set(false);

        this.closeInputStream();
        this.closeOutputStream();
        this.closeSocket();

        this.connectAndWait();

        this.getIoService().execute(() -> {
            this.groupGet();

            this.sendLogEventMessages(new LogState(OnOffToggleStateCalculator.ValidOnOffToggle.ON));
        });

        this.started.set(true);
    }

    @Override
    public boolean isConnected() {
        return this.started.get();
    }

    @Override
    public void disconnect() {
        this.started.set(false);
        this.keepAliveTimer.cancel();
        this.eventListenerTimer.cancel();
        this.getIoService().shutdown();
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
        List<? extends ComponentSpec> components = this.getCentralUnit().getComponents(function).stream().filter(c -> c.getNumber() >= 0).toList();
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
            LOG.debug(() -> String.format("Exception ({}) caught in execute: %s", e.getClass().getName(), e.getMessage()));
            onFailed.accept(message, e);
        }
    }

    private void startEventListener() {
        this.setEventMessageListener(new EventMessageListener());
        this.eventListenerTimer = new Timer("event-listener");
        this.eventListenerTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                TeletaskClientImpl.this.ioService.execute(TeletaskClientImpl.this.getEventMessageListener());
            }
        }, 0, 20);
    }

    private boolean connect(String host, int port) {
        boolean connected = false;

        LOG.info(() -> String.format("(Re)connecting to %s:%s", host, port));

        try {
            this.socket = createSocket(host, port);
            this.socket.setKeepAlive(true);
            this.socket.setSoTimeout(2000);
            connected = true;
        } catch (IOException e) {
            LOG.trace("Problem connecting to host: {}:{}", host, port);
        }

        if (connected) {

            try {
                this.outputStream = this.socket.getOutputStream();
                this.inputStream = this.socket.getInputStream();
            } catch (IOException e) {
                connected = false;
                LOG.trace("Couldn't get I/O for the connection to: {}:{}", host, port);
            }
        }

        if (connected) {
            try {
                this.getCentralUnit().getMessageHandler().getKeepAliveStrategy().execute(this);
            } catch (Exception e) {
                connected = false;
                LOG.trace("Could not send keepalive, assuming not yet connected: {}:{}", host, port);
            }
        }

        if (connected) {
            LOG.info(() -> "Successfully (Re)connected");
        }

        return connected;
    }

    private Socket createSocket(String host, int port) throws IOException {
        return new Socket(host, port);
    }

    private void startKeepAlive() {
        KeepAliveStrategy keepAliveStrategy = this.getCentralUnit().getMessageHandler().getKeepAliveStrategy();
        this.keepAliveTimer = new Timer("keep-alive");
        this.keepAliveTimer.schedule(new KeepAliveService(keepAliveStrategy), 0, keepAliveStrategy.getIntervalMillis());
    }


    private void sendLogEventMessage(Function function, LogState state) {
        new LogMessage(this.getCentralUnit(), function, state).execute(this);
    }

    /**
     * Prevent cloning.
     *
     * @return Nothing really, because this will always result in an Exception.
     * @throws CloneNotSupportedException when called.
     */
    @Override
    public final TeletaskClientImpl clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
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
            } catch (Exception e) {
                LOG.trace(String.format("Exception (%s) caught in EventMessageListener: %s", e.getClass().getName(), e.getMessage()), e);
            }
        }
    }

    public void handleReceiveEvents(Iterable<MessageSupport> messages) {
        List<ComponentSpec> components = new ArrayList<>();
        for (MessageSupport message : messages) {
            if (message instanceof EventMessage) {
                EventMessage eventMessage = (EventMessage) message;
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
        ComponentSpec component = centralUnit.getComponent(eventMessage.getFunction(), eventMessage.getNumber());
        if (component != null) {
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
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug(() -> String.format("Event: %nComponent: not found in configuration %s", eventMessage.getLogInfo(eventMessage.getRawBytes())));
            }
        }
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
        try {
            this.getIoService().shutdown();
            this.getIoService().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.debug(() -> String.format("Exception (%s) caught in stop: %s", e.getClass().getName(), e.getMessage()));
        } finally {
            this.ioService = null;
        }
    }

    private void stopStateChangeListeners() {
        this.getStateChangeListeners().forEach(StateChangeListener::stop);
    }

    private void stopKeepAliveService() {
        this.keepAliveTimer.cancel();
        this.keepAliveTimer.purge();
        this.keepAliveTimer = null;
    }

    private void stopEventListener() {
        this.eventListenerTimer.cancel();
        this.eventListenerTimer.purge();
        this.eventListenerTimer = null;
    }

    private void closeSocket() {
        try {
            this.socket.close();
        } catch (IOException e) {
            LOG.error("Exception ({}) caught in stop: {}", e.getClass().getName(), e.getMessage(), e);
        } finally {
            this.socket = null;
        }
    }

    private void closeOutputStream() {
        try {
            this.getOutputStream().flush();
            this.getOutputStream().close();
        } catch (IOException e) {
            LOG.error("Exception ({}) caught in stop: {}", e.getClass().getName(), e.getMessage(), e);
        } finally {
            this.outputStream = null;
        }
    }

    private void closeInputStream() {
        try {
            this.getInputStream().close();
        } catch (IOException e) {
            LOG.error("Exception ({}) caught in stop: {}", e.getClass().getName(), e.getMessage(), e);
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
            getIoService().execute(() -> {
                TeletaskClientImpl.this.getCentralUnit().getMessageHandler().getGroupGetStrategy().execute(TeletaskClientImpl.this, function, numbers);
            });
        }
    }

    private void startIoService() {
        this.ioService = Executors.newSingleThreadExecutor(r -> new Thread(r, "io"));
    }
}
