package io.github.ridiekel.jeletask.client.builder.composer.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
}
