package io.github.ridiekel.jeletask.client.builder.composer.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.BufferUnderflowException;
import java.util.HexFormat;

class NumberConverterTest {
    private static byte[] hex(String bytes) {
        return HexFormat.of().parseHex(bytes);
    }

    @ParameterizedTest
    @CsvSource({
            "UNSIGNED_BYTE,  00,       0",
            "UNSIGNED_BYTE,  7F,       127",
            "UNSIGNED_BYTE,  80,       128",
            "UNSIGNED_BYTE,  FF,       255",
            "UNSIGNED_SHORT, 0000,     0",
            "UNSIGNED_SHORT, 7FFF,     32767",
            "UNSIGNED_SHORT, 8000,     32768",
            "UNSIGNED_SHORT, FFFF,     65535",
            "UNSIGNED_INT,   00000000, 0",
            "UNSIGNED_INT,   7FFFFFFF, 2147483647",
            "UNSIGNED_INT,   80000000, 2147483648",
            "UNSIGNED_INT,   FFFFFFFF, 4294967295",
    })
    void bytesWithTheHighBitSetAreNotNegative(NumberConverter converter, String bytes, long expected) {
        Assertions.assertThat(converter.convert(hex(bytes)).longValue()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "UNSIGNED_BYTE,  0,          00",
            "UNSIGNED_BYTE,  127,        7F",
            "UNSIGNED_BYTE,  128,        80",
            "UNSIGNED_BYTE,  255,        FF",
            "UNSIGNED_SHORT, 0,          0000",
            "UNSIGNED_SHORT, 255,        00FF",
            "UNSIGNED_SHORT, 256,        0100",
            "UNSIGNED_SHORT, 32768,      8000",
            "UNSIGNED_SHORT, 65535,      FFFF",
            "UNSIGNED_INT,   0,          00000000",
            "UNSIGNED_INT,   2147483647, 7FFFFFFF",
            "UNSIGNED_INT,   2147483648, 80000000",
            "UNSIGNED_INT,   4294967295, FFFFFFFF",
    })
    void numbersAreWrittenBigEndian(NumberConverter converter, long number, String expected) {
        Assertions.assertThat(converter.convert(number)).containsExactly(hex(expected));
        Assertions.assertThat(converter.convert(hex(expected)).longValue()).isEqualTo(number);
    }

    @ParameterizedTest
    @CsvSource({
            "UNSIGNED_BYTE,  256,   00",
            "UNSIGNED_BYTE,  -1,    FF",
            "UNSIGNED_SHORT, 65536, 0000",
            "UNSIGNED_SHORT, -1,    FFFF",
    })
    void numbersOutOfRangeWrapAround(NumberConverter converter, long number, String expected) {
        // Callers that must not wrap (e.g. preset temperatures) have to check the range themselves
        Assertions.assertThat(converter.convert(number)).containsExactly(hex(expected));
    }

    @ParameterizedTest
    @CsvSource({
            "UNSIGNED_BYTE,  255,        FF",
            "UNSIGNED_SHORT, 32767,      7FFF",
            "UNSIGNED_INT,   2147483647, 7FFFFFFF",
    })
    void numbersAreParsedFromText(NumberConverter converter, String text, String expected) {
        Assertions.assertThat(converter.convert(text)).containsExactly(hex(expected));
    }

    @ParameterizedTest
    @CsvSource({
            "UNSIGNED_BYTE,  FF01,         255",
            "UNSIGNED_SHORT, 010203,       258",
            "UNSIGNED_INT,   0000000102,   1",
    })
    void onlyTheLeadingBytesAreReadFromALongerArray(NumberConverter converter, String bytes, long expected) {
        Assertions.assertThat(converter.convert(hex(bytes)).longValue()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "UNSIGNED_SHORT, 01",
            "UNSIGNED_INT,   010203",
    })
    void tooFewBytesAreRejectedInsteadOfGuessed(NumberConverter converter, String bytes) {
        byte[] tooFew = hex(bytes);

        Assertions.assertThatThrownBy(() -> converter.convert(tooFew)).isInstanceOf(BufferUnderflowException.class);
    }

    @Test
    void noBytesAtAllAreRejected() {
        Assertions.assertThatThrownBy(() -> NumberConverter.UNSIGNED_BYTE.convert(new byte[0])).isInstanceOf(BufferUnderflowException.class);
    }

    @Test
    void readTakesExactlyOneValueFromTheGivenIndex() {
        Assertions.assertThat(NumberConverter.UNSIGNED_SHORT.read(hex("AABBCCDD"), 1)).containsExactly(hex("BBCC"));
    }

    @Test
    void readBeyondTheEndIsRejected() {
        byte[] twoBytes = hex("AABB");

        Assertions.assertThatThrownBy(() -> NumberConverter.UNSIGNED_SHORT.read(twoBytes, 1)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @ParameterizedTest
    @CsvSource({"UNSIGNED_BYTE, 1", "UNSIGNED_SHORT, 2", "UNSIGNED_INT, 4"})
    void byteSizeMatchesWhatIsWritten(NumberConverter converter, int expected) {
        Assertions.assertThat(converter.getByteSize()).isEqualTo(expected);
        Assertions.assertThat(converter.convert(0)).hasSize(expected);
    }
}
