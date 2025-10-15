package io.github.ridiekel.jeletask.mockserver;

import io.github.ridiekel.jeletask.TeletaskReceiver;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.DimmerStateCalculator;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.InputStateCalculator;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.MotorStateCalculator;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.builder.message.MessageUtilities;
import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.EventMessage;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.*;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.github.ridiekel.jeletask.mockserver.ExpectationBuilder.WithBuilder.ResponseBuilder.cachedState;
import static io.github.ridiekel.jeletask.mockserver.ExpectationBuilder.WithBuilder.get;
import static io.github.ridiekel.jeletask.mockserver.ExpectationBuilder.WithBuilder.set;
import static io.github.ridiekel.jeletask.mockserver.ExpectationBuilder.*;

public class TeletaskMockServer implements Runnable, TeletaskReceiver {
    private static final Logger LOG = LogManager.getLogger();

    public static final byte[] ACKNOWLEDGE = {10};
    private static final Executor RESPONSE_EXECUTOR = Executors.newSingleThreadExecutor(r -> new Thread(r, "teletask-test-server-responder"));

    private int port;
    private final CentralUnit centralUnit;
    private ServerSocket server;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private final Timer timer = new Timer();
    private Map<MockServerCommand, List<Supplier<MockServerResponse>>> mocks;

    public TeletaskMockServer(int port, CentralUnit centralUnit) {
        this.port = port;
        this.centralUnit = centralUnit;
    }

    public void mock(Consumer<ExpectationBuilder> mockDefinition) {
        ExpectationBuilder expectationBuilder = new ExpectationBuilder(this.centralUnit);

        mockDefinition.accept(expectationBuilder);

        this.mocks = expectationBuilder.getMocks().stream().collect(Collectors.toMap(MockServerExpectation::command, MockServerExpectation::responses));
    }

    @EventListener(classes = {ContextRefreshedEvent.class})
    @Order(50)
    public void start() {
        LOG.debug("Starting teletask test server");

        new Thread(this, "teletask-test-server").start();

        this.mock(e -> {
            mockOnOff(e);
            mockDimmer(e);
            mockMotor(e);
            mockLog(e);
            mockKeepAlive(e);
            mockGet(e);
            mockLightSensor(e);
            mockTemperatureSensor(e);
            mockHumiditySensor(e);
            mockInput(e);
            mockDisplayMessage(e);
            mockSensorGroupGet(e);
        });
    }


    public static java.util.function.Function<String, String> template(String key) {
        return s -> "{\"" + key + "\":\"" + s + "\"}";
    }

    private void mockSensorGroupGet(ExpectationBuilder e) {
        List<? extends ComponentSpec> components = this.centralUnit.getComponents(Function.SENSOR);
        e.when(groupGet(Function.SENSOR, components.stream().mapToInt(ComponentSpec::getNumber).toArray())).thenRespond(
                components.stream().map(component -> ExpectationBuilder.WhenBuilder.state(Function.SENSOR, component.getNumber(), component.getState())).collect(Collectors.toList())
        );
    }

    private void mockLightSensor(ExpectationBuilder e) {
        List<? extends ComponentSpec> components = this.centralUnit.getComponents(Function.SENSOR).stream().filter(c -> c.getType().equalsIgnoreCase("light")).toList();
        components.forEach(c -> {
            c.setState(this.centralUnit.stateFromMessage(c.getFunction(), c.getNumber(), template("state").apply("0")));

            e.with(c.getFunction(), c.getNumber()).when(set(new LuxState(new BigDecimal("3548")))).thenRespond(new LuxState(new BigDecimal("3548")));
            e.with(c.getFunction(), c.getNumber()).when(set(new LuxState(new BigDecimal("794")))).thenRespond(new LuxState(new BigDecimal("794")));
        });
    }

    private void mockTemperatureSensor(ExpectationBuilder e) {
        List<? extends ComponentSpec> components = this.centralUnit.getComponents(Function.SENSOR).stream().filter(c -> c.getType().equalsIgnoreCase("temperature")).toList();
        components.forEach(c -> {
            c.setState(this.centralUnit.stateFromMessage(c.getFunction(), c.getNumber(), template("state").apply("0")));

            for (int i = -200; i <= 300; i++) {
                BigDecimal value = BigDecimal.valueOf(i).divide(BigDecimal.TEN, 1, RoundingMode.HALF_UP);
                e.with(c.getFunction(), c.getNumber()).when(set(new TemperatureState(value))).thenRespond(new TemperatureState(value));
            }
        });
    }

    private void mockHumiditySensor(ExpectationBuilder e) {
        List<? extends ComponentSpec> components = this.centralUnit.getComponents(Function.SENSOR).stream().filter(c -> c.getType().equalsIgnoreCase("humidity")).toList();
        components.forEach(c -> {
            c.setState(this.centralUnit.stateFromMessage(c.getFunction(), c.getNumber(), template("state").apply("0")));

            for (int i = 0; i <= 100; i++) {
                BigDecimal value = BigDecimal.valueOf(i);
                e.with(c.getFunction(), c.getNumber()).when(set(new HumidityState(value))).thenRespond(new HumidityState(value));
            }
        });
    }

    private void mockDisplayMessage(ExpectationBuilder e) {
        List<? extends ComponentSpec> components = this.centralUnit.getComponents(Function.DISPLAYMESSAGE);
        components.forEach(c -> {
            c.setState(this.centralUnit.stateFromMessage(c.getFunction(), c.getNumber(), template("state").apply("0")));
        });
        e.when(groupGet(Function.DISPLAYMESSAGE, components.stream().mapToInt(ComponentSpec::getNumber).toArray())).thenRespond(
                components.stream().map(component -> ExpectationBuilder.WhenBuilder.state(Function.SENSOR, component.getNumber(), component.getState())).collect(Collectors.toList())
        );
    }

    private void mockKeepAlive(ExpectationBuilder e) {
        e.when(keepAlive()).thenRespond();
    }

    private void mockLog(ExpectationBuilder e) {
        Arrays.asList(Function.values()).forEach(function -> e.when(log(function, OnOffToggleStateCalculator.ValidOnOffToggle.ON)).thenRespond());
    }

    private void mockDimmer(ExpectationBuilder e) {
        List<? extends ComponentSpec> components = this.centralUnit.getComponents(Function.DIMMER);
        components.forEach(c -> {
            DimmerState state = new DimmerState(DimmerStateCalculator.ValidDimmerState.ON);
            state.setBrightness(0);
            c.setState(state);
            IntStream.range(0, 101).forEach(i -> {
                DimmerState brightness = new DimmerState(DimmerStateCalculator.ValidDimmerState.ON);
                brightness.setBrightness(i);
                e.with(c.getFunction(), c.getNumber()).when(set(brightness)).thenRespond(brightness);
            });
            e.with(c.getFunction(), c.getNumber()).when(set(new DimmerState(DimmerStateCalculator.ValidDimmerState.OFF))).thenRespond(new DimmerState(0));
        });
        e.when(groupGet(Function.DIMMER, components.stream().mapToInt(ComponentSpec::getNumber).toArray())).thenRespond(
                components.stream().map(component -> ExpectationBuilder.WhenBuilder.state(Function.DIMMER, component.getNumber(), component.getState())).collect(Collectors.toList())
        );
    }

    private void mockGet(ExpectationBuilder e) {
        this.centralUnit.getAllComponents().forEach(c -> e.with(c.getFunction(), c.getNumber()).when(get()).thenRespondFunctional(Collections.singletonList(cachedState())));
    }

    private static @NotNull State<?> responseMotorState(ComponentSpec componentSpec, MotorState requestedState) {
        MotorState responseState = new MotorState();
        responseState.setState(requestedState.getState());
        responseState.setRequestedPosition(requestedState.getRequestedPosition());
        responseState.setCurrentPosition(((MotorState) componentSpec.getState()).getCurrentPosition());
        responseState.setPower(requestedState.getState() == MotorStateCalculator.ValidMotorDirectionState.STOP ? OnOffToggleStateCalculator.ValidOnOffToggle.OFF : OnOffToggleStateCalculator.ValidOnOffToggle.ON);
        if (responseState.getRequestedPosition() != null && responseState.getCurrentPosition() != null) {
            responseState.setSecondsToFinish(new BigDecimal("5.000").divide(new BigDecimal("100.000"), RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(Math.abs(responseState.getCurrentPosition() - responseState.getRequestedPosition()))));
        }
        return responseState;
    }

    private void mockMotor(ExpectationBuilder e) {
        List<? extends ComponentSpec> components = this.centralUnit.getComponents(Function.MOTOR);
        components.forEach(c -> {
            MotorState initial = new MotorState(MotorStateCalculator.ValidMotorDirectionState.STOP);
            initial.setCurrentPosition(0);
            initial.setRequestedPosition(0);
            initial.setPower(OnOffToggleStateCalculator.ValidOnOffToggle.OFF);

            c.setState(initial);

            e.with(c.getFunction(), c.getNumber()).when(set(new MotorState(MotorStateCalculator.ValidMotorDirectionState.UP))).thenRespondFunctional(() -> {
                MotorState requestedState = new MotorState(MotorStateCalculator.ValidMotorDirectionState.UP);
                requestedState.setRequestedPosition(0);
                return responseMotorState(c, requestedState);
            });
            e.with(c.getFunction(), c.getNumber()).when(set(new MotorState(MotorStateCalculator.ValidMotorDirectionState.DOWN))).thenRespondFunctional(() -> {
                MotorState requestedState = new MotorState(MotorStateCalculator.ValidMotorDirectionState.DOWN);
                requestedState.setRequestedPosition(100);
                return responseMotorState(c, requestedState);
            });
            e.with(c.getFunction(), c.getNumber()).when(set(new MotorState(MotorStateCalculator.ValidMotorDirectionState.STOP))).thenRespondFunctional(() -> {
                MotorState requestedState = new MotorState(MotorStateCalculator.ValidMotorDirectionState.STOP);
                requestedState.setPower(OnOffToggleStateCalculator.ValidOnOffToggle.OFF);
                return responseMotorState(c, requestedState);
            });
            IntStream.range(0, 101).forEach(requestedPosition -> {
                MotorState state = new MotorState(MotorStateCalculator.ValidMotorDirectionState.MOTOR_GO_TO_POSITION);
                state.setPower(OnOffToggleStateCalculator.ValidOnOffToggle.ON);
                state.setRequestedPosition(requestedPosition);
                e.with(c.getFunction(), c.getNumber()).when(set(state)).thenRespondFunctional(() -> responseMotorState(c, state));
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
                c.setState(new OnOffState(OnOffToggleStateCalculator.ValidOnOffToggle.ON));
                e.with(c.getFunction(), c.getNumber()).when(set(OnOffToggleStateCalculator.ValidOnOffToggle.OFF)).thenRespond(new OnOffState(OnOffToggleStateCalculator.ValidOnOffToggle.OFF));
                e.with(c.getFunction(), c.getNumber()).when(set(OnOffToggleStateCalculator.ValidOnOffToggle.ON)).thenRespond(new OnOffState(OnOffToggleStateCalculator.ValidOnOffToggle.ON));
            });
            e.when(groupGet(function, components.stream().mapToInt(ComponentSpec::getNumber).toArray())).thenRespond(
                    components.stream().map(component -> ExpectationBuilder.WhenBuilder.state(function, component.getNumber(), component.getState())).collect(Collectors.toList())
            );
        });
    }

    private void mockInput(ExpectationBuilder e) {
        List<? extends ComponentSpec> components = this.centralUnit.getComponents(Function.INPUT);
        components.forEach(c -> {
            c.setState(new InputState(InputStateCalculator.ValidInputState.OPEN));
            e.with(c.getFunction(), c.getNumber()).when(set(InputStateCalculator.ValidInputState.OPEN)).thenRespond(new InputState(InputStateCalculator.ValidInputState.OPEN));
            e.with(c.getFunction(), c.getNumber()).when(set(InputStateCalculator.ValidInputState.CLOSED)).thenRespond(new InputState(InputStateCalculator.ValidInputState.CLOSED));
        });
        e.when(groupGet(Function.INPUT, components.stream().mapToInt(ComponentSpec::getNumber).toArray())).thenRespond(
                components.stream().map(component -> ExpectationBuilder.WhenBuilder.state(Function.INPUT, component.getNumber(), component.getState())).collect(Collectors.toList())
        );
    }

    @Override
    public void run() {
        try {
            this.server = new ServerSocket(this.getPort());
            if (this.getPort() == 0) {
                this.port = server.getLocalPort();
                this.centralUnit.setPort(this.port);
                LOG.info(AnsiOutput.toString(AnsiColor.BRIGHT_MAGENTA, "Server listening to port (random): ", AnsiColor.BRIGHT_WHITE, this.port, AnsiColor.DEFAULT));
            }
            this.socket = this.server.accept();
            this.inputStream = this.socket.getInputStream();
            this.outputStream = this.socket.getOutputStream();
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        List<MessageSupport> messages = MessageUtilities.receive(LOG, TeletaskMockServer.this);
                        for (MessageSupport message : messages) {
                            LOG.trace(() -> String.format("Processing message: %S", message.toString()));
                            TeletaskMockServer.this.outputStream.write(ACKNOWLEDGE);

                            LOG.trace(() -> "Creating mock command");
                            MockServerCommand command = new MockServerCommand(message);
                            LOG.trace(() -> String.format("Created mock command: %s", command));

                            List<Supplier<MockServerResponse>> responses = getMocks().get(command);
                            if (responses != null) {
                                LOG.trace(() -> String.format("Got command responses from mocks. Size: %s", responses.size()));
                                responses.forEach(response -> {
                                    try {
                                        LOG.trace(() -> "Creating event message");
                                        EventMessage eventMessage = response.get().create(centralUnit, message);
                                        LOG.trace(() -> String.format("Created event message %s: ", eventMessage));
                                        RESPONSE_EXECUTOR.execute(() -> {
                                            try {
                                                byte[] rawBytes = eventMessage.getRawBytes();
                                                LOG.trace(() -> String.format("Sending bytes to client: %s", Bytes.bytesToHex(rawBytes)));
                                                TeletaskMockServer.this.outputStream.write(rawBytes);
                                                TeletaskMockServer.this.outputStream.flush();
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        });
                                    } catch (Exception e) {
                                        throw new RuntimeException("Could not create response for command: " + command.command(), e);
                                    }
                                });
                            } else {
                                LOG.warn(AnsiOutput.toString(AnsiColor.YELLOW, "No expectations found for:\n\t", AnsiColor.BRIGHT_YELLOW, "{}"), command);
                            }

                            TeletaskMockServer.this.outputStream.flush();
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

    public Map<MockServerCommand, List<Supplier<MockServerResponse>>> getMocks() {
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
