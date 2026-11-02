package io.github.ridiekel.jeletask.client.spec.state.impl;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.DisplayMessageStateCalculator;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DisplayMessageStateTest {
    private static final DisplayMessageStateCalculator CALCULATOR = new DisplayMessageStateCalculator();

    private static DisplayMessageState ofType(String type) {
        return DisplayMessageState.builder().messageType(type).build();
    }

    /**
     * MESSAGES.md only names "alarm" and "message". TT15132-v3.8 §6.6 defines the byte: 1 for message, 0 for alarm.
     * What happens with no type or an unknown type is characterization.
     */
    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "message,true,1",
            "MESSAGE,true,1",
            "alarm,false,0",
            "ALARM,false,0",
            "Alarm,false,0",
            "null,false,0",
            "doorbell,true,1"
    })
    void messageTypeDecidesTheMessageByte(String type, boolean isMessage, byte messageByte) {
        DisplayMessageState state = ofType(type);

        assertThat(state.isMessage()).isEqualTo(isMessage);
        assertThat(CALCULATOR.toCommand(null, state)[0]).isEqualTo(messageByte);
    }

    @Test
    void readsTheAttributesFromMessagesMd() {
        DisplayMessageState state = CALCULATOR.stateFromMessage("""
                {"message_line1":"DOORBELL","message_line2":"PLEASE OPEN!","message_beeps":"10","message_type":"message"}""");

        assertThat(state.getMessageLine1()).isEqualTo("DOORBELL");
        assertThat(state.getMessageLine2()).isEqualTo("PLEASE OPEN!");
        assertThat(state.getMessageBeeps().intValue()).isEqualTo(10);
        assertThat(state.isMessage()).isTrue();
    }

    @Test
    void withoutAsciiTheMessagesMdExampleIsCutToEightCharactersPerLine() {
        // Characterization: MESSAGES.md promises 16 characters, but ascii defaults to false and §3.6 allows 8 unicode ones
        DisplayMessageState state = CALCULATOR.stateFromMessage("""
                {"message_line1":"DOORBELL","message_line2":"PLEASE OPEN!","message_beeps":"10","message_type":"message"}""");

        assertThat(state.isAscii()).isFalse();
        assertThat(Bytes.bytesToHex(CALCULATOR.toCommand(null, state))).isEqualTo(
                "01 00 "
                        + "00 44 00 4F 00 4F 00 52 00 42 00 45 00 4C 00 4C "
                        + "00 50 00 4C 00 45 00 41 00 53 00 45 00 20 00 4F "
                        + "0A");
    }

    @Test
    void asciiTrueKeepsSixteenCharactersPerLine() {
        DisplayMessageState state = CALCULATOR.stateFromMessage("""
                {"message_line1":"DOORBELL","message_line2":"PLEASE OPEN!","ascii":true}""");

        assertThat(state.isAscii()).isTrue();
        assertThat(Bytes.bytesToHex(CALCULATOR.toCommand(null, state))).isEqualTo(
                "00 01 "
                        + "44 4F 4F 52 42 45 4C 4C 20 20 20 20 20 20 20 20 "
                        + "50 4C 45 41 53 45 20 4F 50 45 4E 21 20 20 20 20 "
                        + "01");
    }

    @Test
    void readsTheBareStateTheMockServerStartsWith() {
        assertThat(CALCULATOR.stateFromMessage("{\"state\":\"0\"}").getState()).isZero();
    }
}
