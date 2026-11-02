package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import io.github.ridiekel.jeletask.client.TestCentralUnit;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator.ValidOnOffToggle;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.DimmerState;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.github.ridiekel.jeletask.client.TestCentralUnit.DIMMER_1;
import static io.github.ridiekel.jeletask.client.TestCentralUnit.RELAY_1;
import static io.github.ridiekel.jeletask.client.TestCentralUnit.RELAY_2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Frames follow TT15132-v3.8 §6 (STX, Length without ChkSm, Command, Parameters, ChkSm as the sum of all bytes) and Set
 * (command 07h): Central Unit, Fnc (§5: relay 01h, dimmer 02h), Number (2 bytes) and State (relay FFh = on, dimmer 0-100).
 */
class SetMessageTest {
    private static final OnOffState ON = new OnOffState(ValidOnOffToggle.ON);
    private static final OnOffState OFF = new OnOffState(ValidOnOffToggle.OFF);

    private final TestCentralUnit unit = TestCentralUnit.on(0);
    private final MessageHandler handler = this.unit.getMessageHandler();

    private SetMessage set(Function function, int number, State<?> state) {
        return new SetMessage(this.unit, function, number, state);
    }

    private String frame(SetMessage message) {
        return Bytes.bytesToHex(this.handler.compose(message.getCommand(), message.getPayload()));
    }

    @Test
    void aRelayIsSwitchedWithFfForOn() {
        assertThat(frame(set(Function.RELAY, RELAY_1, ON))).isEqualTo("02 08 07 01 01 00 01 FF 13");
        assertThat(frame(set(Function.RELAY, RELAY_2, OFF))).isEqualTo("02 08 07 01 01 00 02 00 15");
    }

    @Test
    void aDimmerGetsItsBrightnessAsState() {
        assertThat(frame(set(Function.DIMMER, DIMMER_1, new DimmerState(50)))).isEqualTo("02 08 07 01 02 00 07 32 4D");
    }

    @Test
    void thePayloadIsSplitInFunctionOutputAndState() {
        SetMessage message = set(Function.DIMMER, DIMMER_1, new DimmerState(50));

        assertThat(Bytes.bytesToHex(message.getFunctionBytes())).isEqualTo("02");
        assertThat(Bytes.bytesToHex(message.getOutputBytes())).isEqualTo("00 07");
        assertThat(Bytes.bytesToHex(message.getStateBytes())).isEqualTo("32");
        assertThat(Bytes.bytesToHex(message.getPayload())).isEqualTo("02 00 07 32");
    }

    @Test
    void aSetFrameParsesBackIntoAnEqualMessage() {
        SetMessage message = set(Function.RELAY, RELAY_1, ON);

        assertThat(this.handler.parse(this.unit, Bytes.hexToBytes(frame(message)))).isEqualTo(message).hasSameHashCodeAs(message);
    }

    @Test
    void equalOnlyForTheSameFunctionNumberAndState() {
        SetMessage message = set(Function.RELAY, RELAY_1, ON);
        Object get = new GetMessage(this.unit, Function.RELAY, RELAY_1);

        assertThat(message)
                .isEqualTo(message)
                .isEqualTo(set(Function.RELAY, RELAY_1, ON))
                .hasSameHashCodeAs(set(Function.RELAY, RELAY_1, ON))
                .isNotEqualTo(set(Function.RELAY, RELAY_2, ON))
                .isNotEqualTo(set(Function.RELAY, RELAY_1, OFF))
                .isNotEqualTo(set(Function.DIMMER, RELAY_1, ON))
                .isNotEqualTo(null)
                .isNotEqualTo(get);
    }

    @Test
    void aStateWithinRangeIsValid() {
        assertThat(set(Function.RELAY, RELAY_1, ON).isValid()).isTrue();
        assertThat(set(Function.DIMMER, DIMMER_1, new DimmerState(100)).isValid()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {101, 255})
    void aDimmerAboveHundredPercentIsInvalid(int brightness) {
        assertThat(set(Function.DIMMER, DIMMER_1, new DimmerState(brightness)).isValid()).isFalse();
    }

    @Test
    void withoutStateTheMessageIsInvalid() {
        // MessageSupport.execute then only logs "Invalid request" and sends nothing
        assertThat(set(Function.RELAY, RELAY_1, null).isValid()).isFalse();
    }

    @Test
    void aComponentOutsideTheConfigurationIsOnlyNoticedWhenTheBytesAreNeeded() {
        // Characterization: unlike DisplayMessage, the constructor does not look up the component
        SetMessage message = set(Function.RELAY, 99, ON);

        assertThatThrownBy(message::getPayload).isInstanceOf(CentralUnit.ComponentNotFoundInConfigException.class);
    }

    @Test
    void theIdNamesFunctionAndNumber() {
        assertThat(set(Function.RELAY, RELAY_1, ON).getId()).isEqualTo("SET RELAY(1)");
    }

    @Test
    void toStringShowsFunctionNumberAndStateJson() {
        assertThat(set(Function.RELAY, RELAY_1, ON)).hasToString("SetMessage[function=RELAY,number=1,state={\"state\":\"ON\"}]");
    }

    @Test
    void theLogLineShowsTheDescriptionAndTheState() {
        assertThat(set(Function.RELAY, RELAY_1, ON).toLogLine("SENT"))
                .isEqualTo("[SENT      ] - [RELAY     ] - [  1] - [%40s] - [SET       ] - {\"state\":\"ON\"}".formatted("Component 1"));
    }

    @Test
    void logInfoShowsOnlyTheStateBytesAsState() {
        String[] info = set(Function.DIMMER, DIMMER_1, new DimmerState(50)).getPayloadLogInfo();

        assertThat(info).hasSize(3);
        assertThat(info[0]).isEqualTo("Function: DIMMER | 2 | 02");
        assertThat(info[1]).isEqualTo("Output: 7 | 00 07");
        assertThat(info[2]).startsWith("State: 32 ");
    }
}
