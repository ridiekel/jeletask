package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.mapper;

import io.github.ridiekel.jeletask.client.builder.composer.config.NumberConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MapperTest {

    private Mapper<State> mapper;
    private NumberConverter converter;

    private enum State {
        OFF,
        ON,
        UNKNOWN,
        ALT
    }

    @BeforeEach
    void setUp() {
        converter = NumberConverter.UNSIGNED_INT;
        mapper = new Mapper<>(State.class, converter);
        mapper.add(State.ON, 22);
        mapper.add(State.OFF, 23);
    }

    @Test
    void toString_unknownKey_throws_informativeException() {
        // No mappings added -> any key is unknown
        byte[] bytesFor99 = converter.convert(99);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> mapper.toString(bytesFor99)
        );

        String msg = ex.getMessage();

        assertNotNull(msg);
        assertTrue(msg.contains("No value for key '99'"), "Message should contain the missing key");
        assertTrue(msg.contains(converter.toString()), "Message should include converter class name");
        assertTrue(msg.contains("00 00 00 63"), "Message should contain the hex string");
        assertTrue(msg.contains("{22=ON, 23=OFF}"), "Message should contain the map contents");
        // Do not assert full bytesToHex formatting to avoid coupling to its exact representation
    }

    @Test
    void duplicateNumericMapping_usesLastPutForLookup() {
        Mapper<State> local = new Mapper<>(State.class, converter);

        // Both map to the same numeric key; later one should win for reverse lookup
        local.add(State.OFF, 42);
        local.add(State.ON, 42);

        byte[] fortyTwo = converter.convert(42);

        // Reverse lookup should yield the last added enum
        assertEquals(State.ON, local.toEnum(fortyTwo));
        assertEquals("ON", local.toString(fortyTwo));

        // Forward lookup (toBytes) should be stable and identical for both keys
        // because both were added with the same numeric value
        assertArrayEquals(fortyTwo, local.toBytes(State.OFF));
        assertArrayEquals(fortyTwo, local.toBytes(State.ON));
    }

    @Test
    void toBytes_unmappedEnum_throwsNpeFromConverter() {
        // No mappings for UNKNOWN
        IllegalStateException npe = assertThrows(
                IllegalStateException.class,
                () -> mapper.toBytes(State.UNKNOWN)
        );
        assertEquals("No value for key 'UNKNOWN'. The mapper has following keys: {ON=22, OFF=23}", npe.getMessage());
    }

    private void assertRoundTrip(State state, int numeric) {
        // toBytes should encode the numeric value with our converter
        byte[] expected = converter.convert(numeric);
        byte[] actualBytes = mapper.toBytes(state);
        assertArrayEquals(expected, actualBytes, "toBytes must produce expected bytes");

        // toString should return the enum name
        assertEquals(state.name(), mapper.toString(actualBytes));

        // toEnum should return the enum constant
        assertEquals(state, mapper.toEnum(actualBytes));
    }
}
