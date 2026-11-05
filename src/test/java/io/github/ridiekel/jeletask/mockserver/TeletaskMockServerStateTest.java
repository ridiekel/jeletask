package io.github.ridiekel.jeletask.mockserver;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.DimmerStateCalculator.ValidDimmerState;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.InputStateCalculator.ValidInputState;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.MotorStateCalculator.ValidMotorDirectionState;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator.ValidOnOffToggle;
import io.github.ridiekel.jeletask.client.builder.message.messages.MessageSupport;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.EventMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.GetMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.GroupGetMessage;
import io.github.ridiekel.jeletask.client.builder.message.messages.impl.SetMessage;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.CentralUnitType;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.component.DimmerSpec;
import io.github.ridiekel.jeletask.client.spec.component.InputSpec;
import io.github.ridiekel.jeletask.client.spec.component.LightSensorSpec;
import io.github.ridiekel.jeletask.client.spec.component.LocalMoodSpec;
import io.github.ridiekel.jeletask.client.spec.component.MotorSpec;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.DimmerState;
import io.github.ridiekel.jeletask.client.spec.state.impl.LuxState;
import io.github.ridiekel.jeletask.client.spec.state.impl.MotorState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

/**
 * The mock has to behave like a real central unit: it reports the state it was last set to. The bridge refreshes
 * all states with a periodic group get, so a mock that answers with its initial state instead silently undoes what
 * an end-to-end test just did, and that test then fails now and then, depending on when the refresh happens.
 */
class TeletaskMockServerStateTest {
    private static final int LOCAL_MOOD = 80;
    private static final int OTHER_LOCAL_MOOD = 81;
    private static final int DIMMER = 9;
    private static final int INPUT = 10;
    private static final int LIGHT_SENSOR = 30;
    private static final int MOTOR = 7;

    private CentralUnit centralUnit;
    private TeletaskMockServer server;

    @BeforeEach
    void setUp() {
        centralUnit = new CentralUnit();
        centralUnit.setType(CentralUnitType.MICROS_PLUS);
        centralUnit.setComponentsTypes(Map.of(
                Function.LOCMOOD, List.of(component(new LocalMoodSpec(), LOCAL_MOOD), component(new LocalMoodSpec(), OTHER_LOCAL_MOOD)),
                Function.DIMMER, List.of(component(new DimmerSpec(), DIMMER)),
                Function.INPUT, List.of(component(new InputSpec(), INPUT)),
                Function.SENSOR, List.of(component(new LightSensorSpec(), LIGHT_SENSOR)),
                Function.MOTOR, List.of(component(new MotorSpec(), MOTOR))
        ));

        server = new TeletaskMockServer(0, centralUnit);
        server.mock(server::mockAll);
    }

    @Test
    void groupGetAnswersWithTheInitialStatesWhenNothingWasSet() {
        assertThat(groupGet(Function.LOCMOOD, LOCAL_MOOD)).isEqualTo(ValidOnOffToggle.ON);
        assertThat(groupGet(Function.INPUT, INPUT)).isEqualTo(ValidInputState.OPEN);
    }

    @Test
    void groupGetOfAnOnOffComponentAnswersWithTheLastStateThatWasSet() {
        assertThat(set(Function.LOCMOOD, LOCAL_MOOD, ValidOnOffToggle.OFF)).containsExactly(ValidOnOffToggle.OFF);
        assertThat(groupGet(Function.LOCMOOD, LOCAL_MOOD)).isEqualTo(ValidOnOffToggle.OFF);

        assertThat(set(Function.LOCMOOD, LOCAL_MOOD, ValidOnOffToggle.ON)).containsExactly(ValidOnOffToggle.ON);
        assertThat(groupGet(Function.LOCMOOD, LOCAL_MOOD)).isEqualTo(ValidOnOffToggle.ON);
    }

    @Test
    void settingOneComponentLeavesTheOthersOfTheSameFunctionAlone() {
        set(Function.LOCMOOD, LOCAL_MOOD, ValidOnOffToggle.OFF);

        assertThat(groupGet(Function.LOCMOOD, OTHER_LOCAL_MOOD)).isEqualTo(ValidOnOffToggle.ON);
    }

    @Test
    void getAnswersWithTheLastStateThatWasSet() {
        assertThat(get(Function.LOCMOOD, LOCAL_MOOD)).isEqualTo(ValidOnOffToggle.ON);

        set(Function.LOCMOOD, LOCAL_MOOD, ValidOnOffToggle.OFF);

        assertThat(get(Function.LOCMOOD, LOCAL_MOOD)).isEqualTo(ValidOnOffToggle.OFF);
    }

    @Test
    void groupGetOfADimmerAnswersWithTheLastBrightnessThatWasSet() {
        assertThat(setBrightness(45)).isEqualTo(45);

        assertThat(dimmerOf(groupGetEvent(Function.DIMMER, DIMMER))).isEqualTo(45);
        assertThat(dimmerOf(getEvent(Function.DIMMER, DIMMER))).isEqualTo(45);
    }

    @Test
    void aDimmerTurnedOffIsReportedOffWithBrightnessZero() {
        setBrightness(45);

        respond(new SetMessage(centralUnit, Function.DIMMER, DIMMER, new DimmerState(ValidDimmerState.OFF)));

        DimmerState state = (DimmerState) groupGetEvent(Function.DIMMER, DIMMER).getState();
        assertThat(state.getState()).isEqualTo(ValidDimmerState.OFF);
        assertThat(state.getBrightness()).isZero();
    }

    @Test
    void aDimmerTurnedOnAgainGoesBackToItsPreviousBrightness() {
        setBrightness(45);
        respond(new SetMessage(centralUnit, Function.DIMMER, DIMMER, new DimmerState(ValidDimmerState.OFF)));

        // 103 is "previous state" in the Teletask protocol
        List<EventMessage> events = respond(new SetMessage(centralUnit, Function.DIMMER, DIMMER, new DimmerState(ValidDimmerState.ON, 103)));

        assertThat(events).singleElement().extracting(TeletaskMockServerStateTest::dimmerOf).isEqualTo(45);
        assertThat(dimmerOf(groupGetEvent(Function.DIMMER, DIMMER))).isEqualTo(45);
    }

    @Test
    void aDimmerTurnedOnWithoutAPreviousBrightnessGoesToFull() {
        List<EventMessage> events = respond(new SetMessage(centralUnit, Function.DIMMER, DIMMER, new DimmerState(ValidDimmerState.ON, 103)));

        assertThat(events).singleElement().extracting(TeletaskMockServerStateTest::dimmerOf).isEqualTo(100);
    }

    @Test
    void groupGetOfAnInputAnswersWithTheLastStateThatWasSet() {
        assertThat(set(Function.INPUT, INPUT, ValidInputState.CLOSED)).containsExactly(ValidInputState.CLOSED);

        assertThat(groupGet(Function.INPUT, INPUT)).isEqualTo(ValidInputState.CLOSED);
    }

    @Test
    void aSensorEchoesAnyValueItIsSetToAndRemembersIt() {
        // 1234 lux has no mock of its own, the mock echoes every sensor value back
        List<EventMessage> events = respond(new SetMessage(centralUnit, Function.SENSOR, LIGHT_SENSOR, new LuxState(new BigDecimal("1234"))));

        // The protocol stores lux as a rounded logarithm, so the value comes back close to, not equal to, 1234
        BigDecimal reported = (BigDecimal) events.getFirst().getState().getState();
        assertThat(reported).isCloseTo(new BigDecimal("1234"), withinPercentage(5));
        assertThat(groupGet(Function.SENSOR, LIGHT_SENSOR)).isEqualTo(reported);
    }

    @Test
    void groupGetOfAMotorAnswersWithTheRunItIsIn() {
        respond(new SetMessage(centralUnit, Function.MOTOR, MOTOR, new MotorState(ValidMotorDirectionState.DOWN)));

        // A full run takes five seconds, so the motor is still going down to 100 right after it was started
        MotorState state = (MotorState) groupGetEvent(Function.MOTOR, MOTOR).getState();
        assertThat(state.getState()).isEqualTo(ValidMotorDirectionState.DOWN);
        assertThat(state.getRequestedPosition()).isEqualTo(100);
    }

    @Test
    void mockingAgainStartsFromTheInitialStates() {
        set(Function.LOCMOOD, LOCAL_MOOD, ValidOnOffToggle.OFF);

        server.mock(server::mockAll);

        assertThat(groupGet(Function.LOCMOOD, LOCAL_MOOD)).isEqualTo(ValidOnOffToggle.ON);
    }

    @Test
    void aCommandWithoutAMockGetsNoResponse() {
        // Toggle is not mocked: the bridge always sends ON or OFF
        assertThat(set(Function.LOCMOOD, LOCAL_MOOD, ValidOnOffToggle.TOGGLE)).isEmpty();

        assertThat(groupGet(Function.LOCMOOD, LOCAL_MOOD)).isEqualTo(ValidOnOffToggle.ON);
    }

    @Test
    void theEventOfAStateThatWasSetMatchesTheTeletaskProtocol() {
        List<EventMessage> events = respond(new SetMessage(centralUnit, Function.LOCMOOD, OTHER_LOCAL_MOOD, stateOf(Function.LOCMOOD, OTHER_LOCAL_MOOD, ValidOnOffToggle.OFF)));

        // TT15132-v3.8: event (10) of local mood (08) 81 (00 51), state 00 = OFF, followed by the checksum
        assertThat(events).singleElement().extracting(e -> Bytes.bytesToHex(e.getRawBytes())).isEqualTo("02 09 10 01 08 00 51 00 00 75");
    }

    private static ComponentSpec component(ComponentSpec component, int number) {
        component.setNumber(number);
        component.setDescription(component.getClass().getSimpleName() + " " + number);
        return component;
    }

    private State<?> stateOf(Function function, int number, Enum<?> state) {
        // The same way the mock builds the state it expects, see ExpectationBuilder.WithBuilder.set(Enum)
        return centralUnit.stateFromMessage(function, number, TeletaskMockServer.template("state").apply(state.toString()));
    }

    private List<Object> set(Function function, int number, Enum<?> state) {
        return respond(new SetMessage(centralUnit, function, number, stateOf(function, number, state))).stream()
                .map(e -> (Object) e.getState().getState())
                .toList();
    }

    private Integer setBrightness(int brightness) {
        DimmerState state = new DimmerState(ValidDimmerState.ON);
        state.setBrightness(brightness);
        return dimmerOf(respond(new SetMessage(centralUnit, Function.DIMMER, DIMMER, state)).getFirst());
    }

    private Object groupGet(Function function, int number) {
        return groupGetEvent(function, number).getState().getState();
    }

    private EventMessage groupGetEvent(Function function, int number) {
        int[] numbers = centralUnit.getComponents(function).filter(c -> c.getNumber() >= 0).mapToInt(ComponentSpec::getNumber).toArray();
        return respond(new GroupGetMessage(centralUnit, function, numbers)).stream()
                .filter(e -> e.getNumber() == number)
                .findFirst()
                .orElseThrow(noEventFor(function, number));
    }

    private Object get(Function function, int number) {
        return getEvent(function, number).getState().getState();
    }

    private EventMessage getEvent(Function function, int number) {
        return respond(new GetMessage(centralUnit, function, number)).stream()
                .findFirst()
                .orElseThrow(noEventFor(function, number));
    }

    private List<EventMessage> respond(MessageSupport message) {
        return server.respondTo(message);
    }

    private static Integer dimmerOf(EventMessage event) {
        return ((DimmerState) event.getState()).getBrightness();
    }

    private static Supplier<AssertionError> noEventFor(Function function, int number) {
        return () -> new AssertionError(String.format("No event for %s %s", function, number));
    }
}
