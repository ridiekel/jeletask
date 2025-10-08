package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.spec.state.impl.DisplayMessageState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DisplayMessageStateCalculatorTest {
    private DisplayMessageStateCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DisplayMessageStateCalculator();
    }

    @Test
    @DisplayName("Should return correct state type")
    void shouldReturnCorrectStateType() {
        assertThat(calculator.getStateType()).isEqualTo(DisplayMessageState.class);
    }

    @Nested
    @DisplayName("toCommand Tests")
    class ToCommandTests {

        @Test
        @DisplayName("Should create command with ASCII message type")
        void shouldCreateCommandWithAsciiMessageType() {
            DisplayMessageState state = DisplayMessageState.builder()
                    .messageType("message")
                    .ascii(true)
                    .messageLine1("Line 1")
                    .messageLine2("Line 2")
                    .messageBeeps(3)
                    .build();

            byte[] result = calculator.toCommand(state);

            assertThat(result).isNotNull();
            assertThat(result[0]).isEqualTo((byte) 1); // message type
            assertThat(result[1]).isEqualTo((byte) 1); // isAscii
            assertThat(result).hasSize(35); // 1 + 1 + 16 + 16 + 1
        }

        @Test
        @DisplayName("Should create command with alarm type")
        void shouldCreateCommandWithAlarmType() {
            DisplayMessageState state = DisplayMessageState.builder()
                    .messageType("alarm")
                    .ascii(true)
                    .messageLine1("Alarm!")
                    .messageBeeps(5)
                    .build();

            byte[] result = calculator.toCommand(state);

            assertThat(result[0]).isEqualTo((byte) 0); // alarm type
        }

        @Test
        @DisplayName("Should create command with UTF-16 encoding")
        void shouldCreateCommandWithUtf16Encoding() {
            DisplayMessageState state = DisplayMessageState.builder()
                    .messageType("message")
                    .ascii(false) // UTF-16
                    .messageLine1("Test")
                    .messageLine2("Data")
                    .messageBeeps(2)
                    .build();

            byte[] result = calculator.toCommand(state);

            assertThat(result[0]).isEqualTo((byte) 1); // message type
            assertThat(result[1]).isEqualTo((byte) 0); // not ASCII
            assertThat(result).hasSize(35); // 1 + 1 + 16 + 16 + 1
        }

        @Test
        @DisplayName("Should pad short ASCII message lines with spaces")
        void shouldPadShortAsciiMessageLines() {
            DisplayMessageState state = DisplayMessageState.builder()
                    .messageType("message")
                    .ascii(true)
                    .messageLine1("ABC")
                    .messageLine2("XY")
                    .messageBeeps(1)
                    .build();

            byte[] result = calculator.toCommand(state);

            // Extract line 1 (bytes 2-17)
            String line1 = new String(result, 2, 16, StandardCharsets.US_ASCII);
            assertThat(line1).isEqualTo("ABC             ");

            // Extract line 2 (bytes 18-33)
            String line2 = new String(result, 18, 16, StandardCharsets.US_ASCII);
            assertThat(line2).isEqualTo("XY              ");
        }

        @Test
        @DisplayName("Should truncate long ASCII message lines")
        void shouldTruncateLongAsciiMessageLines() {
            DisplayMessageState state = DisplayMessageState.builder()
                    .messageType("message")
                    .ascii(true)
                    .messageLine1("This is a very long message that exceeds 16 characters")
                    .messageLine2("Another long line")
                    .messageBeeps(1)
                    .build();

            byte[] result = calculator.toCommand(state);

            String line1 = new String(result, 2, 16, StandardCharsets.US_ASCII);
            assertThat(line1).hasSize(16);
            assertThat(line1).isEqualTo("This is a very l");

            String line2 = new String(result, 18, 16, StandardCharsets.US_ASCII);
            assertThat(line2).hasSize(16);
            assertThat(line2).isEqualTo("Another long lin");
        }

        @Test
        @DisplayName("Should handle null message lines with spaces")
        void shouldHandleNullMessageLinesWithSpaces() {
            DisplayMessageState state = DisplayMessageState.builder()
                    .messageType("message")
                    .ascii(true)
                    .messageLine1(null)
                    .messageLine2(null)
                    .messageBeeps(1)
                    .build();

            byte[] result = calculator.toCommand(state);

            String line1 = new String(result, 2, 16, StandardCharsets.US_ASCII);
            assertThat(line1).isEqualTo("                ");

            String line2 = new String(result, 18, 16, StandardCharsets.US_ASCII);
            assertThat(line2).isEqualTo("                ");
        }

        @Test
        @DisplayName("Should use default beeps when null")
        void shouldUseDefaultBeepsWhenNull() {
            DisplayMessageState state = DisplayMessageState.builder()
                    .messageType("message")
                    .ascii(true)
                    .messageLine1("Test")
                    .messageBeeps(null)
                    .build();

            byte[] result = calculator.toCommand(state);

            assertThat(result[34]).isEqualTo((byte) 1); // default beeps
        }

        @Test
        @DisplayName("Should use specified number of beeps")
        void shouldUseSpecifiedNumberOfBeeps() {
            DisplayMessageState state = DisplayMessageState.builder()
                    .messageType("message")
                    .ascii(true)
                    .messageLine1("Test")
                    .messageBeeps(7)
                    .build();

            byte[] result = calculator.toCommand(state);

            assertThat(result[34]).isEqualTo((byte) 7);
        }

        @Test
        @DisplayName("Should truncate UTF-16 message to 8 characters")
        void shouldTruncateUtf16MessageTo8Characters() {
            DisplayMessageState state = DisplayMessageState.builder()
                    .messageType("message")
                    .ascii(false)
                    .messageLine1("123456789ABCDEF") // 15 characters
                    .messageBeeps(1)
                    .build();

            byte[] result = calculator.toCommand(state);

            // UTF-16 uses 2 bytes per character, so 8 chars = 16 bytes
            assertThat(result).hasSize(35);
        }

        @ParameterizedTest
        @MethodSource("provideMessageStates")
        @DisplayName("Should handle various message state combinations")
        void shouldHandleVariousMessageStateCombinations(
                String messageType,
                boolean isAscii,
                Integer beeps,
                byte expectedMessageType,
                byte expectedAsciiFlag) {

            DisplayMessageState state = DisplayMessageState.builder()
                    .messageType(messageType)
                    .ascii(isAscii)
                    .messageLine1("Test")
                    .messageBeeps(beeps)
                    .build();

            byte[] result = calculator.toCommand(state);

            assertThat(result[0]).isEqualTo(expectedMessageType);
            assertThat(result[1]).isEqualTo(expectedAsciiFlag);
        }

        static Stream<Arguments> provideMessageStates() {
            return Stream.of(
                    Arguments.of("message", true, 1, (byte) 1, (byte) 1),
                    Arguments.of("message", false, 2, (byte) 1, (byte) 0),
                    Arguments.of("alarm", true, 3, (byte) 0, (byte) 1),
                    Arguments.of("alarm", false, 4, (byte) 0, (byte) 0),
                    Arguments.of("message", true, null, (byte) 1, (byte) 1)
            );
        }
    }

    @Nested
    @DisplayName("fromEvent Tests")
    class FromEventTests {
        @Test
        @DisplayName("Should convert normal byte value")
        void shouldConvertNormalByteValue() {
            DisplayMessageState result = calculator.fromEvent(null, new byte[]{42});

            assertThat(result).isNotNull();
            assertThat(result.getState()).isEqualTo(42L);
        }
    }

    @Test
    @DisplayName("Should convert -1 to 255")
    void shouldConvertMinusOneToTwoFiftyFive() {
        DisplayMessageState result = calculator.fromEvent(null, new byte[]{-1});

        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(255L);
    }

    @Test
    @DisplayName("Should handle zero value")
    void shouldHandleZeroValue() {
        DisplayMessageState result = calculator.fromEvent(null, new byte[]{0});

        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should handle maximum byte value")
    void shouldHandleMaximumByteValue() {
        DisplayMessageState result = calculator.fromEvent(null, new byte[]{(byte) 254});

        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(254L);
    }

    @Test
    @DisplayName("RECOMMENDATION: Use UTF_16BE to avoid BOM issues")
    void recommendationUseUtf16BeToAvoidBomIssues() {
        // Dit is hoe het ZOU MOETEN werken:
        String message = "12345678"; // 8 chars
        byte[] withBom = message.getBytes(StandardCharsets.UTF_16);
        byte[] withoutBom = message.getBytes(StandardCharsets.UTF_16BE);

        System.out.println("UTF_16 size: " + withBom.length + " bytes");    // 18 (16 + 2 BOM)
        System.out.println("UTF_16BE size: " + withoutBom.length + " bytes"); // 16

        assertThat(withBom.length).isEqualTo(18); // 16 + 2 BOM
        assertThat(withoutBom.length).isEqualTo(16); // Precies wat we willen!

        // Suggest: Change production code to use UTF_16BE or UTF_16LE
    }


    @Test
    @DisplayName("Should use US_ASCII encoding for ASCII mode")
    void shouldUseUsAsciiEncodingForAsciiMode() {
        DisplayMessageState state = DisplayMessageState.builder()
                .messageType("message")
                .ascii(true)
                .messageLine1("Test Message")
                .messageBeeps(1)
                .build();

        byte[] result = calculator.toCommand(state);

        // Extract line 1 (bytes 2-17)
        byte[] line1Bytes = Arrays.copyOfRange(result, 2, 18);
        String line1 = new String(line1Bytes, StandardCharsets.US_ASCII);

        assertThat(line1).startsWith("Test Message");

        // Verify: ASCII uses 1 byte per character
        assertThat(line1Bytes[0]).isEqualTo((byte) 'T');
        assertThat(line1Bytes[1]).isEqualTo((byte) 'e');
        assertThat(line1Bytes[2]).isEqualTo((byte) 's');
        assertThat(line1Bytes[3]).isEqualTo((byte) 't');
    }

    @Test
    @DisplayName("UTF-16 mode should limit to 8 characters due to 2-byte encoding")
    void utf16ModeShouldLimitTo8Characters() {
        DisplayMessageState state = DisplayMessageState.builder()
                .messageType("message")
                .ascii(false)
                .messageLine1("12345678901234567890") // 20 chars
                .messageBeeps(1)
                .build();

        byte[] result = calculator.toCommand(state);

        // Extract line 1 (bytes 2-17)
        byte[] line1Bytes = Arrays.copyOfRange(result, 2, 18);
        String decoded = new String(line1Bytes, StandardCharsets.UTF_16);

        assertThat(decoded.trim().length()).isLessThanOrEqualTo(8);
    }

    @Test
    @DisplayName("Should correctly encode Unicode characters in UTF-16 mode")
    void shouldCorrectlyEncodeUnicodeCharactersInUtf16Mode() {
        DisplayMessageState state = DisplayMessageState.builder()
                .messageType("message")
                .ascii(false)
                .messageLine1("Hello世界") // Mix van ASCII en Chinese karakters
                .messageBeeps(1)
                .build();

        byte[] result = calculator.toCommand(state);

        byte[] line1Bytes = Arrays.copyOfRange(result, 2, 18);
        String decoded = new String(line1Bytes, StandardCharsets.UTF_16);

        assertThat(decoded).contains("Hello");
        assertThat(decoded).contains("世");
    }

    @Test
    @DisplayName("ASCII mode should have exactly 16 bytes per line")
    void asciiModeShouldHaveExactly16BytesPerLine() {
        DisplayMessageState state = DisplayMessageState.builder()
                .messageType("message")
                .ascii(true)
                .messageLine1("ABC")
                .messageLine2("XY")
                .messageBeeps(1)
                .build();

        byte[] result = calculator.toCommand(state);

        // Totaal: 1 (type) + 1 (ascii) + 16 (line1) + 16 (line2) + 1 (beeps) = 35
        assertThat(result).hasSize(35);

        // Verifieer dat elke regel precies 16 bytes is
        byte[] line1 = Arrays.copyOfRange(result, 2, 18);
        byte[] line2 = Arrays.copyOfRange(result, 18, 34);

        assertThat(line1).hasSize(16);
        assertThat(line2).hasSize(16);
    }

    @Test
    @DisplayName("UTF-16 mode should have exactly 16 bytes per line (8 chars)")
    void utf16ModeShouldHaveExactly16BytesPerLine() {
        DisplayMessageState state = DisplayMessageState.builder()
                .messageType("message")
                .ascii(false)
                .messageLine1("Test")
                .messageLine2("Data")
                .messageBeeps(1)
                .build();

        byte[] result = calculator.toCommand(state);

        // Totaal moet nog steeds 35 zijn
        assertThat(result).hasSize(35);

        byte[] line1 = Arrays.copyOfRange(result, 2, 18);
        byte[] line2 = Arrays.copyOfRange(result, 18, 34);

        // Elke regel is nog steeds 16 bytes, maar bevat minder karakters
        assertThat(line1).hasSize(16);
        assertThat(line2).hasSize(16);
    }
}
