package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator.ValidOnOffToggle;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.CentralUnitType;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;
import io.github.ridiekel.jeletask.client.spec.state.impl.PulseCounterState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Frames follow TT15132-v3.8 §6 (STX, Length without ChkSm, Command, Parameters, ChkSm) and §6.5 (Event Report, command
 * 10h: Central Unit, Fnc, Number (2 bytes), ErrorState, States). Fnc values come from §5. The manual has no literal
 * event frame: they were worked out by hand, and "RELAY_3_OFF" equals the one in the comment of MicrosPlusMessageHandler.
 */
class EventMessageTest {
    private static final String RELAY_3_OFF = "02 09 10 01 01 00 03 00 00 20";
    private static final String RELAY_3_ON = "02 09 10 01 01 00 03 00 FF 1F";
    private static final String RELAY_1000_ON = "02 09 10 01 01 03 E8 00 FF 07";
    // The report from the comment in PulseCounterStateCalculator, with 1000000 pulses as total
    private static final String PULSE_COUNTER_6 = "02 1C 10 01 14 00 06 00 06 3A 00 00 00 00 00 00 00 1A 00 00 FF 00 00 00 00 0F 42 40 33";

    private CentralUnit centralUnit;
    private MessageHandler handler;

    @BeforeEach
    void setUp() {
        ComponentSpec pulseCounter = new ComponentSpec();
        pulseCounter.setNumber(6);
        pulseCounter.setType("PULSECOUNTER");

        ComponentSpec relay3 = new ComponentSpec();
        relay3.setNumber(3);
        relay3.setDescription("Kitchen");
        relay3.setState(new OnOffState(ValidOnOffToggle.ON));

        ComponentSpec relay1000 = new ComponentSpec();
        relay1000.setNumber(1000);

        Map<Function, List<ComponentSpec>> components = new LinkedHashMap<>();
        components.put(Function.RELAY, List.of(relay3, relay1000));
        components.put(Function.SENSOR, List.of(pulseCounter));

        centralUnit = new CentralUnit();
        centralUnit.setType(CentralUnitType.MICROS_PLUS);
        centralUnit.setComponentsTypes(components);
        handler = centralUnit.getMessageHandler();
    }

    private EventMessage parse(String frame) {
        return (EventMessage) handler.parse(centralUnit, Bytes.hexToBytes(frame));
    }

    @Test
    void parsesFunctionNumberAndState() {
        EventMessage off = parse(RELAY_3_OFF);
        EventMessage on = parse(RELAY_3_ON);

        assertThat(off.getFunction()).isEqualTo(Function.RELAY);
        assertThat(off.getNumber()).isEqualTo(3);
        assertThat(off.getState()).isEqualTo(new OnOffState(ValidOnOffToggle.OFF));
        assertThat(on.getState()).isEqualTo(new OnOffState(ValidOnOffToggle.ON));
    }

    @Test
    void keepsTheRawBytesItWasParsedFrom() {
        assertThat(Bytes.bytesToHex(parse(RELAY_3_ON).getRawBytes())).isEqualTo(RELAY_3_ON);
    }

    @Test
    void numberIsReadHighByteFirst() {
        assertThat(parse(RELAY_1000_ON).getNumber()).isEqualTo(1000);
    }

    @Test
    void payloadIsFunctionAndNumberOnly() {
        // Characterization: the manual has no such thing, and leaving out the states means it is no way to rebuild the frame
        assertThat(Bytes.bytesToHex(parse(RELAY_1000_ON).getPayload())).isEqualTo("01 03 E8");
    }

    @Test
    void payloadUsesTheFunctionNumberOfTheManual() {
        assertThat(Bytes.bytesToHex(parse(PULSE_COUNTER_6).getPayload())).isEqualTo("14 00 06");
    }

    @Test
    void parsesAPulseCounterReport() {
        EventMessage event = parse(PULSE_COUNTER_6);

        assertThat(event.getFunction()).isEqualTo(Function.SENSOR);
        assertThat(event.getNumber()).isEqualTo(6);
        assertThat(event.getState()).isInstanceOf(PulseCounterState.class);
        PulseCounterState state = (PulseCounterState) event.getState();
        assertThat(state.getCurrent()).isEqualTo((short) 1594);
        assertThat(state.getTotal()).isEqualTo(1000f);
    }

    @Test
    void aWrongChecksumIsRejected() {
        assertThatThrownBy(() -> parse("02 09 10 01 01 00 03 00 FF 20"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Checksum not correct. Received '20' but expected '1F'");
    }

    @Test
    void aComponentThatIsNotConfiguredIsNotFound() {
        // 02 09 10 01 01 00 04 00 FF 20: relay 4 is not in the config
        assertThatThrownBy(() -> parse("02 09 10 01 01 00 04 00 FF 20"))
                .isInstanceOf(CentralUnit.ComponentNotFoundInConfigException.class);
    }

    @Test
    void logInfoShowsOnlyTheStateBytesBetweenHeaderAndChecksum() {
        String[] pulse = parse(PULSE_COUNTER_6).getPayloadLogInfo();
        String[] relay = parse(RELAY_1000_ON).getPayloadLogInfo();

        assertThat(relay[0]).isEqualTo("Function: RELAY | 1 | 01");
        assertThat(relay[1]).isEqualTo("Output: 1000 | 03 E8");
        // The JSON of the state follows directly, so a trailing checksum byte would break the match
        assertThat(relay[2]).matches("(?s)State: FF\\s+\\{.*");
        assertThat(pulse[2]).matches("(?s)State: 06 3A 00 00 00 00 00 00 00 1A 00 00 FF 00 00 00 00 0F 42 40\\s+\\{.*");
    }

    @Test
    void logInfoCanBeBuiltFromTheRawBytes() {
        EventMessage event = parse(RELAY_3_ON);

        assertThat(event.getLogInfo(event.getRawBytes())).contains("Command: EVENT").contains("Function: RELAY | 1");
    }

    @Test
    void logLineShowsTheConfiguredDescription() {
        assertThat(parse(RELAY_3_ON).toLogLine("RECEIVED")).contains("RELAY").contains("EVENT").contains("Kitchen");
    }

    @Test
    void anEventRoundTripsThroughTheMessageHandler() {
        OnOffState off = new OnOffState(ValidOnOffToggle.OFF);

        EventMessage created = handler.createResponseEventMessageForTesting(centralUnit, Function.RELAY, new MessageHandler.OutputState(3, off));
        EventMessage parsed = (EventMessage) handler.parse(centralUnit, created.getRawBytes());

        assertThat(Bytes.bytesToHex(created.getRawBytes())).isEqualTo(RELAY_3_OFF);
        assertThat(parsed.getFunction()).isEqualTo(Function.RELAY);
        assertThat(parsed.getNumber()).isEqualTo(3);
        assertThat(parsed.getState()).isEqualTo(off);
    }

    @Test
    void parsingTheSameFrameTwiceGivesEqualEvents() {
        assertThat(parse(RELAY_3_ON)).isEqualTo(parse(RELAY_3_ON));
        assertThat(parse(RELAY_3_ON)).isNotEqualTo(parse(RELAY_3_OFF));
        assertThat(parse(RELAY_3_ON)).isNotEqualTo(parse(RELAY_1000_ON));
    }
}
