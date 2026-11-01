package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.client.spec.state.impl.PulseCounterState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sensor report layout from TT15132-v3.8 §3.9: Value (0-1), Target (2-3), Day (4-5), Night (6-7), Standby (8),
 * Active Preset (9), Mode (10), SpeedMode (11), Power (12), Frost (13), not used (14), Direction (15),
 * Total number of pulses (16-19). §6: the higher byte is sent first. §6.8.5: total = int32 / pulses per unit.
 * The manual does not give Standby a size; one byte is what makes Active Preset land on 9 in the example below.
 */
class PulseCounterStateCalculatorTest {
    private static final PulseCounterStateCalculator CALCULATOR = new PulseCounterStateCalculator();

    private static ComponentSpec component(int pulsesPerUnit) {
        ComponentSpec component = new ComponentSpec();
        component.setPulses_per_unit(pulsesPerUnit);
        return component;
    }

    /**
     * The 20 bytes of a report, of which only the value and the total matter.
     */
    private static byte[] report(String value, String total) {
        return Bytes.hexToBytes(value + " 00 00 00 00 00 00 00 1A 00 00 FF 00 00 00 " + total);
    }

    private static float total(int pulsesPerUnit, String total) {
        return CALCULATOR.fromEvent(component(pulsesPerUnit), report("00 00", total)).getTotal().floatValue();
    }

    private static Number current(String value) {
        return CALCULATOR.fromEvent(component(1000), report(value, "00 00 00 00")).getCurrent();
    }

    @Test
    void readsTheExampleInTheCalculatorComment() {
        byte[] example = Bytes.hexToBytes("06 3A 00 00 00 00 00 00 00 1A 00 00 FF 00 00 00 00 00 00 00 F9");

        PulseCounterState state = CALCULATOR.fromEvent(component(1000), example);

        assertThat(state.getState()).isEqualTo("PULSECOUNTER");
        assertThat(state.getCurrent()).isEqualTo((short) 0x063A);
        assertThat(state.getTotal()).isEqualTo(0f);
    }

    @Test
    void ignoresEveryByteOutsideValueAndTotal() {
        byte[] noise = Bytes.hexToBytes("06 3A AA AA AA AA AA AA AA AA AA AA AA AA AA AA 00 00 03 E8");

        PulseCounterState state = CALCULATOR.fromEvent(component(1000), noise);

        assertThat(state.getCurrent()).isEqualTo((short) 1594);
        assertThat(state.getTotal()).isEqualTo(1f);
    }

    @ParameterizedTest
    @CsvSource({
            "00 01,1",
            "01 00,256",
            "7F FF,32767",
    })
    void currentIsTheBigEndianValue(String value, short expected) {
        assertThat(current(value)).isEqualTo(expected);
    }

    /**
     * Characterization: §6.8 calls anything from SENS_MAX (0x3F00) on an error, the code does not treat it as one.
     * Above 32767 the unsigned value is cut back to a signed short, so a counter would report negative numbers.
     */
    @ParameterizedTest
    @CsvSource({
            "80 00,-32768",
            "9C 40,-25536",
            "FF FF,-1",
    })
    void currentAbove32767TurnsNegative(String value, short expected) {
        assertThat(current(value)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "1000,00 0F 42 40,1000.0",
            "1000,00 00 04 D2,1.234",
            "1,00 00 01 00,256.0",
            "1,01 00 00 00,16777216.0",
            "2,00 00 00 03,1.5",
    })
    void totalIsTheBigEndianCountDividedByPulsesPerUnit(int pulsesPerUnit, String total, float expected) {
        assertThat(total(pulsesPerUnit, total)).isEqualTo(expected);
    }

    @Test
    void totalIsNeverNegative() {
        // §6.8.5 says int32, but a count of pulses cannot be negative, so the full four bytes are read as unsigned
        assertThat(total(1, "FF FF FF FF")).isEqualTo(4294967295f);
        assertThat(total(1000, "80 00 00 00")).isEqualTo(2147483.75f);
    }

    @Test
    void totalLosesPrecisionAboveTwoToThe24() {
        // Characterization: the count goes through a float, so 16777217 pulses read as 16777216
        assertThat(total(1, "01 00 00 01")).isEqualTo(16777216f);
    }

    @Test
    void aReportTooShortForTheTotalFails() {
        // Characterization: §3.9 gives other sensors only two bytes after Direction, which would land here
        byte[] withoutTotal = Bytes.hexToBytes("06 3A 00 00 00 00 00 00 00 1A 00 00 FF 00 00 00 00 00");
        ComponentSpec component = component(1000);

        assertThatThrownBy(() -> CALCULATOR.fromEvent(component, withoutTotal))
                .isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }

    @Test
    void zeroPulsesPerUnitGivesInfinityOrNaN() throws JsonProcessingException {
        // Characterization: nothing validates the setting, §6.8.5 only says to divide by it
        PulseCounterState infinite = CALCULATOR.fromEvent(component(0), report("00 00", "00 00 00 01"));
        PulseCounterState notANumber = CALCULATOR.fromEvent(component(0), report("00 00", "00 00 00 00"));

        assertThat(infinite.getTotal()).isEqualTo(Float.POSITIVE_INFINITY);
        assertThat(notANumber.getTotal()).isEqualTo(Float.NaN);
        // That reaches MQTT as a string where a number is expected
        assertThat(State.OBJECT_MAPPER.readTree(infinite.toString()).get("total").isTextual()).isTrue();
        assertThat(State.OBJECT_MAPPER.readTree(infinite.toString()).get("total").asText()).isEqualTo("Infinity");
    }

    @Test
    void pulsesPerUnitDefaultsToThousandWhenNotConfigured() throws JsonProcessingException {
        assertThat(sensorFromConfig("").getPulses_per_unit()).isEqualTo(1000);
    }

    @Test
    void aNullPulsesPerUnitInTheConfigBecomesZero() throws JsonProcessingException {
        // Characterization: the field is a primitive int, so the mapper of CentralUnitFactory turns null into 0
        assertThat(sensorFromConfig(", \"pulses_per_unit\": null").getPulses_per_unit()).isZero();
    }

    private static ComponentSpec sensorFromConfig(String extraAttribute) throws JsonProcessingException {
        CentralUnit centralUnit = new ObjectMapper().readValue("""
                {"type": "MICROS_PLUS", "componentsTypes": {"SENSOR": [
                  {"number": 6, "description": "Pulse counter", "type": "PULSECOUNTER"%s}
                ]}}""".formatted(extraAttribute), CentralUnit.class);
        return centralUnit.getComponent(Function.SENSOR, 6);
    }

    @Test
    void cannotBeWrittenTo() {
        ComponentSpec component = component(1000);
        PulseCounterState state = new PulseCounterState("PULSECOUNTER");

        assertThatThrownBy(() -> CALCULATOR.toCommand(component, state))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("read only");
    }

    @Test
    void jsonOfTheStateCarriesWhatHomeAssistantReads() throws JsonProcessingException {
        // HASensorConfig publishes the template "{{ value_json.current }}"
        PulseCounterState state = CALCULATOR.fromEvent(component(1000), report("06 3A", "00 0F 42 40"));

        JsonNode json = State.OBJECT_MAPPER.readTree(state.toString());

        assertThat(json.get("state").asText()).isEqualTo("PULSECOUNTER");
        assertThat(json.get("current").asInt()).isEqualTo(1594);
        assertThat(json.get("total").asDouble()).isEqualTo(1000d);
    }

    @Test
    void readsCurrentAndTotalFromJson() {
        PulseCounterState state = CALCULATOR.stateFromMessage("{\"state\":\"PULSECOUNTER\",\"current\":1594,\"total\":2.5}");

        assertThat(state.getState()).isEqualTo("PULSECOUNTER");
        assertThat(state.getCurrent().intValue()).isEqualTo(1594);
        assertThat(state.getTotal().doubleValue()).isEqualTo(2.5d);
    }

    @Test
    void aChangeInCurrentOrTotalIsAChangeOfState() {
        // TeletaskClientImpl compares states with equals to decide whether anything changed
        PulseCounterState state = CALCULATOR.fromEvent(component(1000), report("00 01", "00 00 00 01"));
        PulseCounterState same = CALCULATOR.fromEvent(component(1000), report("00 01", "00 00 00 01"));
        PulseCounterState otherCurrent = CALCULATOR.fromEvent(component(1000), report("00 02", "00 00 00 01"));
        PulseCounterState otherTotal = CALCULATOR.fromEvent(component(1000), report("00 01", "00 00 00 02"));

        assertThat(state)
                .isEqualTo(same)
                .hasSameHashCodeAs(same)
                .isNotEqualTo(otherCurrent)
                .isNotEqualTo(otherTotal);
    }
}
