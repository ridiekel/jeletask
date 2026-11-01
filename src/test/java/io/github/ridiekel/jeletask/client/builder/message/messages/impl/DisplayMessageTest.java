package io.github.ridiekel.jeletask.client.builder.message.messages.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandler;
import io.github.ridiekel.jeletask.client.builder.composer.MessageHandlerFactory;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.CentralUnitType;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.impl.DisplayMessageState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Expected frames follow TT15132-v3.8 §6 (frame format, checksum) and §6.6 (Write Display Message, command 04h).
 * The manual has no literal display frame, so they were worked out by hand from that layout.
 */
class DisplayMessageTest {
    private static final MessageHandler HANDLER = MessageHandlerFactory.getMessageHandler(CentralUnitType.MICROS_PLUS);
    private static final int NUMBER = 1000;

    private static CentralUnit centralUnit(String bus, String address) {
        String json = """
                {"type": "MICROS_PLUS", "componentsTypes": {"DISPLAYMESSAGE": [
                  {"number": %d, "description": "Aurus", "bus_numbers": %s, "address_numbers": %s}
                ]}}""".formatted(NUMBER, quoted(bus), quoted(address));
        try {
            // Same mapper as CentralUnitFactory, so the config attribute names are covered too
            return new ObjectMapper().readValue(json, CentralUnit.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String quoted(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static DisplayMessage message(CentralUnit centralUnit, DisplayMessageState state) {
        return new DisplayMessage(centralUnit, Function.DISPLAYMESSAGE, NUMBER, state);
    }

    private static DisplayMessage message(String bus, String address) {
        return message(centralUnit(bus, address), alarm());
    }

    /**
     * The call from §3.6: Message = false, Ascii = true, Beeps = 10.
     */
    private static DisplayMessageState alarm() {
        return DisplayMessageState.builder()
                .messageType("alarm")
                .ascii(true)
                .messageLine1("DOORBELL")
                .messageLine2("PLEASE OPEN!")
                .messageBeeps(10)
                .build();
    }

    private static byte[] rawFrame(DisplayMessage message) {
        return HANDLER.compose(message.getCommand(), message.getPayload());
    }

    private static String frame(DisplayMessage message) {
        return Bytes.bytesToHex(rawFrame(message));
    }

    @Test
    void twoInterfacesFromTheManualExample() {
        // §3.6: BusNr = {1, 2}, Adres = {11, 7}
        assertThat(frame(message("1,2", "11,7"))).isEqualTo(
                "02 2B 04 01 "
                        + "01 02 0B 07 "
                        + "00 01 "
                        + "44 4F 4F 52 42 45 4C 4C 20 20 20 20 20 20 20 20 "
                        + "50 4C 45 41 53 45 20 4F 50 45 4E 21 20 20 20 20 "
                        + "0A 52");
    }

    @Test
    void allInterfacesAreAddressedWithBusAndAddressZero() {
        // §3.6: "To create a Message/Alarm on all interfaces: set BusNr = {0}, Adres = {0}"
        DisplayMessageState state = DisplayMessageState.builder()
                .messageType("message")
                .ascii(true)
                .messageLine1("HELLO")
                .messageBeeps(1)
                .build();

        assertThat(frame(message(centralUnit("0", "0"), state))).isEqualTo(
                "02 29 04 01 "
                        + "00 00 "
                        + "01 01 "
                        + "48 45 4C 4C 4F 20 20 20 20 20 20 20 20 20 20 20 "
                        + "20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 "
                        + "01 07");
    }

    @Test
    void unicodeLinesFillTheSameSixteenBytes() {
        // §3.6 says "UTF-16" without a byte order; big endian is characterization of the current code
        DisplayMessageState state = DisplayMessageState.builder()
                .messageType("message")
                .ascii(false)
                .messageLine1("Café")
                .messageLine2("世界")
                .messageBeeps(3)
                .build();

        assertThat(frame(message(centralUnit("1,1", "13,5"), state))).isEqualTo(
                "02 2B 04 01 "
                        + "01 01 0D 05 "
                        + "01 00 "
                        + "00 43 00 61 00 66 00 E9 00 20 00 20 00 20 00 20 "
                        + "4E 16 75 4C 00 20 00 20 00 20 00 20 00 20 00 20 "
                        + "03 A2");
    }

    @Test
    void payloadLeavesCommandAndCentralUnitToTheHandler() {
        assertThat(Bytes.bytesToHex(message("1,2", "11,7").getPayload())).isEqualTo(
                "01 02 0B 07 "
                        + "00 01 "
                        + "44 4F 4F 52 42 45 4C 4C 20 20 20 20 20 20 20 20 "
                        + "50 4C 45 41 53 45 20 4F 50 45 4E 21 20 20 20 20 "
                        + "0A");
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {"null,'11,7'", "'1,2',null", "null,null"})
    void missingBusOrAddressNumbersAreRejected(String bus, String address) {
        assertThatThrownBy(() -> message(bus, address))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Address or bus numbers are missing");
    }

    @ParameterizedTest
    @CsvSource({"'1,2',11", "1,'11,7'"})
    void listsOfDifferentLengthAreRejected(String bus, String address) {
        assertThatThrownBy(() -> message(bus, address))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Address and bus numbers configuration error");
    }

    /**
     * Characterization: the manual does not limit these values or describe the config syntax.
     * Spaces after a comma and values above 127 are refused, even though a parameter is a full byte on the wire.
     */
    @ParameterizedTest
    @CsvSource({"'1, 2','11, 7'", "200,11", "1,200", "x,11", "'',''"})
    void numbersThatDoNotParseAsSignedByteAreRejected(String bus, String address) {
        assertThatThrownBy(() -> message(bus, address)).isExactlyInstanceOf(NumberFormatException.class);
    }

    @Test
    void anUnknownComponentIsRejected() {
        CentralUnit centralUnit = centralUnit("1", "11");

        assertThatThrownBy(() -> new DisplayMessage(centralUnit, Function.DISPLAYMESSAGE, NUMBER + 1, alarm()))
                .isInstanceOf(CentralUnit.ComponentNotFoundInConfigException.class);
    }

    @Test
    void aMessageWithAStateIsValid() {
        assertThat(message("1", "11").isValid()).isTrue();
    }

    @Test
    void logInfoNamesTheFunctionAndOnlyTheDisplayBytesAsState() {
        String[] info = message("1,2", "11,7").getPayloadLogInfo();

        assertThat(info[0]).isEqualTo("Function: DISPLAYMESSAGE | 54 | 36");
        assertThat(info[1]).isEqualTo("Output: 1000 | 03 E8");
        assertThat(info[2]).startsWith("State: 00 01 44 4F");
    }

    @Test
    void logInfoCanBeBuiltForAFrameWithMoreParametersThanNames() {
        // Only "Central Unit" has a name, the other columns must fall back on "Param n"
        DisplayMessage message = message("1,2", "11,7");

        assertThat(message.getLogInfo(rawFrame(message))).contains("Command: DISPLAYMESSAGE").contains("Function: DISPLAYMESSAGE | 54");
    }

    @Test
    void logLineShowsTheConfiguredDescription() {
        assertThat(message("1", "11").toLogLine("SENT")).contains("DISPLAYMESSAGE").contains("1000").contains("Aurus");
    }

    @Test
    void aDisplayFrameCannotBeParsedBack() {
        // Characterization: §6.6 is CCT to TDS only, and DisplayMessageCommandConfigurable.parse returns null
        assertThat(HANDLER.parse(centralUnit("1,2", "11,7"), rawFrame(message("1,2", "11,7")))).isNull();
    }
}
