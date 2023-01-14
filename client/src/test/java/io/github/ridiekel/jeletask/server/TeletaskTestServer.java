package io.github.ridiekel.jeletask.server;

import io.github.ridiekel.jeletask.TeletaskReceiver;
import io.github.ridiekel.jeletask.client.builder.message.MessageUtilities;
import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.EventMessage;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.ComponentState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.github.ridiekel.jeletask.server.ExpectationBuilder.WithBuilder.ResponseBuilder.cachedState;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.WithBuilder.get;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.WithBuilder.set;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.groupGet;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.keepAlive;
import static io.github.ridiekel.jeletask.server.ExpectationBuilder.log;

public class TeletaskTestServer implements Runnable, TeletaskReceiver {
    /**
     * Logger responsible for logging and debugging statements.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TeletaskTestServer.class);
    public static final byte[] ACKNOWLEDGE = {10};
    private static final Executor RESPONSE_EXECUTOR = Executors.newSingleThreadExecutor(r -> new Thread(r, "teletask-test-server-responder"));

    private final int port;
    private final CentralUnit centralUnit;
    private ServerSocket server;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private final Timer timer = new Timer();
    private Map<TestServerCommand, List<TestServerResponse>> mocks;

    public TeletaskTestServer(int port, CentralUnit centralUnit) {
        this.port = port;
        this.centralUnit = centralUnit;
    }

    public void mock(Consumer<ExpectationBuilder> mockDefinition) {
        ExpectationBuilder expectationBuilder = new ExpectationBuilder(this.centralUnit);

        mockDefinition.accept(expectationBuilder);

        this.mocks = expectationBuilder.getMocks().stream().collect(Collectors.toMap(TestServerExpectation::command, TestServerExpectation::responses));
    }

    @EventListener(classes = {ContextRefreshedEvent.class})
    @Order(50)
    public void start() {
        LOG.debug("Starting teletask test server");

        this.mock(e -> {
            mockOnOff(e);
            mockDimmer(e);
            mockMotor(e);
            mockLog(e);
            mockKeepAlive(e);
            mockGet(e);
            mockSensor(e);
            mockInput(e);
        });

        new Thread(this, "teletask-test-server").start();
    }

    private void mockGet(ExpectationBuilder e) {
        this.centralUnit.getAllComponents().forEach(c -> e.with(c.getFunction(), c.getNumber()).when(get()).thenRespond(cachedState()));
    }

    private void mockSensor(ExpectationBuilder e) {
        List<? extends ComponentSpec> components = this.centralUnit.getComponents(Function.SENSOR);
        components.forEach(c -> {
            c.setState(new ComponentState("0"));
        });
        e.when(groupGet(Function.SENSOR, components.stream().mapToInt(ComponentSpec::getNumber).toArray())).thenRespond(
                components.stream().map(component -> ExpectationBuilder.WhenBuilder.state(Function.SENSOR, component.getNumber(), component.getState())).collect(Collectors.toList())
        );
    }

    private void mockKeepAlive(ExpectationBuilder e) {
        e.when(keepAlive()).thenRespond();
    }

    private void mockLog(ExpectationBuilder e) {
        Arrays.asList(Function.values()).forEach(function -> e.when(log(function, "ON")).thenRespond());
    }

    private void mockDimmer(ExpectationBuilder e) {
        List<? extends ComponentSpec> components = this.centralUnit.getComponents(Function.DIMMER);
        components.forEach(c -> {
            c.setState(new ComponentState("0"));
//            e.with(c.getFunction(), c.getNumber()).when(set("OFF")).thenRespond("0");
//            e.with(c.getFunction(), c.getNumber()).when(set("ON")).thenRespond("100");
            IntStream.range(0, 101).mapToObj(String::valueOf).forEach(i -> e.with(c.getFunction(), c.getNumber()).when(set(i)).thenRespond(i));
        });
        e.when(groupGet(Function.DIMMER, components.stream().mapToInt(ComponentSpec::getNumber).toArray())).thenRespond(
                components.stream().map(component -> ExpectationBuilder.WhenBuilder.state(Function.DIMMER, component.getNumber(), component.getState())).collect(Collectors.toList())
        );
    }

    private void mockMotor(ExpectationBuilder e) {
        List<? extends ComponentSpec> components = this.centralUnit.getComponents(Function.MOTOR);
        components.forEach(c -> {
            c.setState(new ComponentState("UP"));
            e.with(c.getFunction(), c.getNumber()).when(set("UP")).thenRespond("UP");
            e.with(c.getFunction(), c.getNumber()).when(set("DOWN")).thenRespond("DOWN");
            e.with(c.getFunction(), c.getNumber()).when(set("STOP")).thenRespond("STOP");
            IntStream.range(0, 101).forEach(i -> {
                ComponentState state = new ComponentState();
                state.setPosition(i);
                e.with(c.getFunction(), c.getNumber()).when(set(String.valueOf(i))).thenRespond(state);
            });
        });
        e.when(groupGet(Function.MOTOR, components.stream().mapToInt(ComponentSpec::getNumber).toArray())).thenRespond(
                components.stream().map(component -> ExpectationBuilder.WhenBuilder.state(Function.MOTOR, component.getNumber(), component.getState())).collect(Collectors.toList())
        );
    }

    private void mockOnOff(ExpectationBuilder e) {
        List.of(Function.RELAY,
                Function.LOCMOOD,
                Function.GENMOOD,
                Function.TIMEDFNC,
                Function.COND,
                Function.FLAG,
                Function.TIMEDMOOD).forEach(function -> {
            List<? extends ComponentSpec> components = this.centralUnit.getComponents(function);
            components.forEach(c -> {
                c.setState(new ComponentState("ON"));
                e.with(c.getFunction(), c.getNumber()).when(set("OFF")).thenRespond("OFF");
                e.with(c.getFunction(), c.getNumber()).when(set("ON")).thenRespond("ON");
            });
            e.when(groupGet(function, components.stream().mapToInt(ComponentSpec::getNumber).toArray())).thenRespond(
                    components.stream().map(component -> ExpectationBuilder.WhenBuilder.state(function, component.getNumber(), component.getState())).collect(Collectors.toList())
            );
        });
    }

    private void mockInput(ExpectationBuilder e) {
            List<? extends ComponentSpec> components = this.centralUnit.getComponents(Function.INPUT);
            components.forEach(c -> c.setState(new ComponentState("OPEN")));
            e.when(groupGet(Function.INPUT, components.stream().mapToInt(ComponentSpec::getNumber).toArray())).thenRespond(
                    components.stream().map(component -> ExpectationBuilder.WhenBuilder.state(Function.INPUT, component.getNumber(), component.getState())).collect(Collectors.toList())
            );
    }

    @Override
    public void run() {
        try {
            this.server = new ServerSocket(this.getPort());
            this.socket = this.server.accept();
            this.inputStream = this.socket.getInputStream();
            this.outputStream = this.socket.getOutputStream();
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        List<MessageSupport> messages = MessageUtilities.receive(LOG, TeletaskTestServer.this);
                        for (MessageSupport message : messages) {
                            LOG.debug("Processing message: {}", message.toString());
                            TeletaskTestServer.this.outputStream.write(ACKNOWLEDGE);

                            TestServerCommand command = new TestServerCommand(message);

                            List<TestServerResponse> responses = getMocks().get(command);
                            if (responses != null) {
                                responses.forEach(response -> {
                                    try {
                                        EventMessage eventMessage = response.create(centralUnit, message);
                                        RESPONSE_EXECUTOR.execute(() -> {
                                            try {
                                                LOG.debug("Sending bytes to client: {}", Bytes.bytesToHex(eventMessage.getRawBytes()));
                                                TeletaskTestServer.this.outputStream.write(eventMessage.getRawBytes());
                                                TeletaskTestServer.this.outputStream.flush();
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        });
                                    } catch (Exception e) {
                                        throw new RuntimeException("Could not create response for command: " + command.command(), e);
                                    }
                                });
                            } else {
                                LOG.debug("No expectations found for:\n\t{} in \n\t\t{}", command, getMocks().keySet().stream().map(Objects::toString).collect(Collectors.joining("\n\t\t")));
                            }

                            TeletaskTestServer.this.outputStream.flush();
                        }
                    } catch (Exception e) {
                        LOG.error("Exception ({}) caught in run: {}", e.getClass().getName(), e.getMessage(), e);
                    }
                }
            }, 100, 10);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        LOG.debug("Stopping test server...");
        try {
            this.timer.purge();
            this.timer.cancel();
            this.inputStream.close();
            this.outputStream.close();
            this.socket.close();
            this.server.close();
        } catch (IOException e) {
            LOG.error("Exception ({}) caught in stop: {}", e.getClass().getName(), e.getMessage(), e);
        }
        LOG.debug("Stopped test server.");
    }

    public Map<TestServerCommand, List<TestServerResponse>> getMocks() {
        return Optional.ofNullable(mocks).orElseGet(Map::of);
    }

    public int getPort() {
        return this.port;
    }

    @Override
    public InputStream getInputStream() {
        return this.inputStream;
    }

    @Override
    public CentralUnit getCentralUnit() {
        return this.centralUnit;
    }
}
