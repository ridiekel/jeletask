package io.github.ridiekel.jeletask.client.spec.config;

import io.github.ridiekel.jeletask.client.spec.CentralUnit;
import io.github.ridiekel.jeletask.client.spec.CentralUnitType;
import io.github.ridiekel.jeletask.client.spec.Function;
import io.github.ridiekel.jeletask.client.spec.component.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CentralUnitConfigReaderTest {
    private static CentralUnitConfigReader.Result read(String json) {
        return CentralUnitConfigReader.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), "test.json");
    }

    private static CentralUnitConfigReader.Result components(String function, String components) {
        return read("""
                {"type": "MICROS_PLUS", "componentsTypes": {"%s": [%s]}}""".formatted(function, components));
    }

    private static InvalidConfigException invalid(String function, String components) {
        return invalid(() -> components(function, components));
    }

    private static InvalidConfigException invalid(Runnable read) {
        Throwable thrown = catchThrowable(read::run);
        assertThat(thrown).isInstanceOf(InvalidConfigException.class);
        return (InvalidConfigException) thrown;
    }

    @Test
    void everyFunctionGetsItsOwnClass() {
        CentralUnit centralUnit = read("""
                {"type": "MICROS_PLUS", "componentsTypes": {
                  "RELAY": [{"number": 1}], "DIMMER": [{"number": 1}], "MOTOR": [{"number": 1}],
                  "LOCMOOD": [{"number": 1}], "TIMEDMOOD": [{"number": 1}], "GENMOOD": [{"number": 1}],
                  "FLAG": [{"number": 1}], "COND": [{"number": 1}], "INPUT": [{"number": 1}], "TIMEDFNC": [{"number": 1}],
                  "DISPLAYMESSAGE": [{"number": 1, "bus_numbers": "1", "address_numbers": "2"}],
                  "SENSOR": [
                    {"number": 1, "type": "TEMPERATURE"}, {"number": 2, "type": "LIGHT"}, {"number": 3, "type": "HUMIDITY"},
                    {"number": 4, "type": "GAS", "gas_type": "0-10v", "gas_max": 10}, {"number": 5, "type": "TEMPERATURECONTROL"},
                    {"number": 6, "type": "PULSECOUNTER"}
                  ]
                }}""").centralUnit();

        assertThat(centralUnit.getCentralUnitType()).isEqualTo(CentralUnitType.MICROS_PLUS);
        assertThat(centralUnit.getComponent(Function.RELAY, 1)).isInstanceOf(RelaySpec.class);
        assertThat(centralUnit.getComponent(Function.DIMMER, 1)).isInstanceOf(DimmerSpec.class);
        assertThat(centralUnit.getComponent(Function.MOTOR, 1)).isInstanceOf(MotorSpec.class);
        assertThat(centralUnit.getComponent(Function.LOCMOOD, 1)).isInstanceOf(LocalMoodSpec.class);
        assertThat(centralUnit.getComponent(Function.TIMEDMOOD, 1)).isInstanceOf(TimedMoodSpec.class);
        assertThat(centralUnit.getComponent(Function.GENMOOD, 1)).isInstanceOf(GeneralMoodSpec.class);
        assertThat(centralUnit.getComponent(Function.FLAG, 1)).isInstanceOf(FlagSpec.class);
        assertThat(centralUnit.getComponent(Function.COND, 1)).isInstanceOf(ConditionSpec.class);
        assertThat(centralUnit.getComponent(Function.INPUT, 1)).isInstanceOf(InputSpec.class);
        assertThat(centralUnit.getComponent(Function.TIMEDFNC, 1)).isInstanceOf(TimedFunctionSpec.class);
        assertThat(centralUnit.getComponent(Function.DISPLAYMESSAGE, 1)).isInstanceOf(DisplayMessageSpec.class);
        assertThat(centralUnit.getComponent(Function.SENSOR, 1)).isInstanceOf(TemperatureSensorSpec.class);
        assertThat(centralUnit.getComponent(Function.SENSOR, 2)).isInstanceOf(LightSensorSpec.class);
        assertThat(centralUnit.getComponent(Function.SENSOR, 3)).isInstanceOf(HumiditySensorSpec.class);
        assertThat(centralUnit.getComponent(Function.SENSOR, 4)).isInstanceOf(GasSensorSpec.class);
        assertThat(centralUnit.getComponent(Function.SENSOR, 5)).isInstanceOf(TemperatureControlSpec.class);
        assertThat(centralUnit.getComponent(Function.SENSOR, 6)).isInstanceOf(PulseCounterSpec.class);
    }

    @Test
    void theCommonPropertiesAreReadForEveryFunction() {
        RelaySpec relay = (RelaySpec) components("RELAY", """
                {"number": 3, "description": "Kitchen", "device": "Lights", "suggested_area": "Kitchen area", "haType": "switch", "type": "light"}
                """).centralUnit().getComponent(Function.RELAY, 3);

        assertThat(relay.getDescription()).isEqualTo("Kitchen");
        assertThat(relay.getDevice()).isEqualTo("Lights");
        assertThat(relay.getSuggestedArea()).isEqualTo("Kitchen area");
        assertThat(relay.getHaType()).isEqualTo("switch");
        assertThat(relay.getType()).isEqualTo("light");
    }

    @Test
    void theOldSpellingOfHaTypeStillWorks() {
        assertThat(components("RELAY", """
                {"number": 3, "hatype": "switch"}""").centralUnit().getComponent(Function.RELAY, 3).getHaType()).isEqualTo("switch");
    }

    @Test
    void theSensorSpecificPropertiesAreRead() {
        CentralUnit centralUnit = components("SENSOR", """
                {"number": 1, "type": "GAS", "gas_type": "4-20ma", "gas_min": -5, "gas_max": 14.5, "decimals": 2, "ha_unit_of_measurement": "bar"},
                {"number": 2, "type": "TEMPERATURECONTROL", "ha_modes": "heat,off", "ha_min_temp": 5, "ha_max_temp": 25,
                 "ha_temperature_step": 0.1, "ha_fan_modes": "", "ha_preset_modes": "Comfort=DAY", "ha_hvac_action": true, "decimals": 0},
                {"number": 3, "type": "PULSECOUNTER", "pulses_per_unit": 500}
                """).centralUnit();

        GasSensorSpec gas = (GasSensorSpec) centralUnit.getComponent(Function.SENSOR, 1);
        assertThat(gas.getGasType()).isEqualTo("4-20ma");
        assertThat(gas.getGasMin()).isEqualByComparingTo("-5");
        assertThat(gas.getGasMax()).isEqualByComparingTo(new BigDecimal("14.5"));
        assertThat(gas.getDecimals()).isEqualTo(2);
        assertThat(gas.getHaUnitOfMeasurement()).isEqualTo("bar");

        TemperatureControlSpec thermostat = (TemperatureControlSpec) centralUnit.getComponent(Function.SENSOR, 2);
        assertThat(thermostat.getHaModes()).isEqualTo("heat,off");
        assertThat(thermostat.getHaMinTemp()).isEqualTo(5);
        assertThat(thermostat.getHaMaxTemp()).isEqualTo(25);
        assertThat(thermostat.getHaTemperatureStep()).isEqualTo(0.1);
        assertThat(thermostat.getHaFanModes()).isEmpty();
        assertThat(thermostat.getHaPresetModes()).isEqualTo("Comfort=DAY");
        assertThat(thermostat.isHaHvacAction()).isTrue();
        assertThat(thermostat.getDecimals()).isZero();

        assertThat(((PulseCounterSpec) centralUnit.getComponent(Function.SENSOR, 3)).getPulsesPerUnit()).isEqualTo(500);
    }

    @Test
    void functionsAndSensorTypesAreCaseInsensitive() {
        CentralUnitConfigReader.Result result = read("""
                {"type": "micros_plus", "componentsTypes": {"relay": [{"number": 1}], "sensor": [{"number": 1, "type": "temperature"}]}}""");

        assertThat(result.centralUnit().getComponent(Function.RELAY, 1)).isInstanceOf(RelaySpec.class);
        assertThat(result.centralUnit().getComponent(Function.SENSOR, 1).getType()).isEqualTo("TEMPERATURE");
    }

    @Test
    void aPropertyOfAnotherFunctionIsAWarningAndIgnored() {
        CentralUnitConfigReader.Result result = components("RELAY", """
                {"number": 10, "description": "Light", "gas_min": 0, "long_press_duration_millis": 500}""");

        assertThat(result.centralUnit().getComponent(Function.RELAY, 10)).isInstanceOf(RelaySpec.class);
        assertThat(result.warnings()).containsExactly(
                "RELAY #1 (number 10, \"Light\"): 'gas_min' is not a property of a RELAY, it is ignored (known properties: description, device, haType, number, suggested_area, type)",
                "RELAY #1 (number 10, \"Light\"): 'long_press_duration_millis' is not a property of a RELAY, it is ignored (known properties: description, device, haType, number, suggested_area, type)"
        );
    }

    @Test
    void aPropertyOfAnotherSensorTypeIsAWarning() {
        CentralUnitConfigReader.Result result = components("SENSOR", """
                {"number": 31, "type": "TEMPERATURE", "pulses_per_unit": 10}""");

        assertThat(result.warnings()).singleElement().asString()
                .startsWith("SENSOR #1 (number 31): 'pulses_per_unit' is not a property of a SENSOR of type TEMPERATURE, it is ignored")
                .contains("decimals", "ha_unit_of_measurement");
    }

    @Test
    void anUnknownTopLevelPropertyIsAWarning() {
        assertThat(read("""
                {"type": "MICROS_PLUS", "host": "192.168.0.1", "componentsTypes": {}}""").warnings())
                .containsExactly("'host' is not a property of the config file, it is ignored (known properties: [type, componentsTypes])");
    }

    @Test
    void allErrorsAreReportedTogether() {
        InvalidConfigException e = invalid(() -> read("""
                {"type": "MICROS_PLUS", "componentsTypes": {
                  "RELAY": [{"number": 1, "type": "cover"}, {"description": "No number"}],
                  "INPUT": [{"number": 2, "long_press_duration_millis": 0}],
                  "SENSOR": [{"number": 3, "type": "GAS", "gas_type": "4-20ma", "gas_min": 10, "gas_max": 10}]
                }}"""));

        assertThat(e.getErrors()).containsExactly(
                "RELAY #1 (number 1): 'type' 'cover' is not supported for a RELAY, expected one of [light, switch, scene, binary_sensor]",
                "RELAY #2 (\"No number\"): 'number' is required",
                "INPUT #1 (number 2): 'long_press_duration_millis' must be greater than 0, but is 0",
                "SENSOR #1 (number 3): 'gas_max' must be greater than gas_min, but gas_min is 10 and gas_max is 10"
        );
        assertThat(e.getMessage()).startsWith("The config file test.json has 4 problems:" + System.lineSeparator() + "  - RELAY #1 (number 1): ");
    }

    @Test
    void aWrongValueTypeNamesTheProperty() {
        assertThat(invalid("RELAY", """
                {"number": "ten"}""").getErrors())
                .containsExactly("RELAY #1 (number ten): 'number' \"ten\" is not a whole number");
        assertThat(invalid("SENSOR", """
                {"number": 1, "type": "TEMPERATURECONTROL", "ha_hvac_action": "maybe"}""").getErrors())
                .containsExactly("SENSOR #1 (number 1): 'ha_hvac_action' \"maybe\" is not true or false");
    }

    @Test
    void aNumberCanBeUsedOnlyOncePerFunction() {
        assertThat(invalid("FLAG", """
                {"number": 5, "description": "First"}, {"number": 5, "description": "Second"}""").getErrors())
                .containsExactly("FLAG #2 (number 5, \"Second\"): 'number' 5 is already used by FLAG #1 (number 5, \"First\")");
        // Another function may use the same number
        assertThat(read("""
                {"type": "MICROS_PLUS", "componentsTypes": {"FLAG": [{"number": 5}], "RELAY": [{"number": 5}]}}""").warnings()).isEmpty();
    }

    @Test
    void aNegativeNumberIsReservedForTheBridge() {
        assertThat(invalid("RELAY", """
                {"number": -1}""").getErrors())
                .containsExactly("RELAY #1 (number -1): 'number' must not be negative, negative numbers are reserved for the bridge and its devices");
    }

    @Test
    void anInvalidHaTypeIsAnError() {
        assertThat(invalid("DIMMER", """
                {"number": 1, "haType": "lamp"}""").getErrors()).singleElement().asString()
                .isEqualTo("DIMMER #1 (number 1): 'haType' 'lamp' is not a Home Assistant type, expected one of [binary_sensor, sensor, light, climate, switch, scene, cover]");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '~', value = {
            "{\"number\": 1}                         | SENSOR #1 (number 1): 'type' is required for a SENSOR, expected one of [TEMPERATURE, LIGHT, HUMIDITY, GAS, TEMPERATURECONTROL, PULSECOUNTER]",
            "{\"number\": 1, \"type\": \"STRING\"}   | SENSOR #1 (number 1): 'type' \"STRING\" is not a sensor type, expected one of [TEMPERATURE, LIGHT, HUMIDITY, GAS, TEMPERATURECONTROL, PULSECOUNTER]",
            "{\"number\": 1, \"type\": \"WIND\"}     | SENSOR #1 (number 1): 'type' \"WIND\" is not a sensor type, expected one of [TEMPERATURE, LIGHT, HUMIDITY, GAS, TEMPERATURECONTROL, PULSECOUNTER]",
    })
    void aSensorNeedsAKnownType(String sensor, String error) {
        assertThat(invalid("SENSOR", sensor).getErrors()).containsExactly(error);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '~', value = {
            "\"gas_max\": 10                                       | 'gas_type' is required, expected one of [0-10v, 0-20ma, 4-20ma, 5-10v]",
            "\"gas_type\": \"4-20mA\", \"gas_max\": 10             | 'gas_type' '4-20mA' is not a gas type, expected one of [0-10v, 0-20ma, 4-20ma, 5-10v]",
            "\"gas_type\": \"0-10v\"                               | 'gas_max' must be greater than gas_min, but gas_min is 0 and gas_max is 0",
            "\"gas_type\": \"0-10v\", \"gas_max\": null            | 'gas_max' is required",
            "\"gas_type\": \"0-10v\", \"gas_max\": 10, \"decimals\": -1 | 'decimals' must be 0 or more, but is -1",
    })
    void aGasSensorIsValidated(String properties, String error) {
        assertThat(invalid("SENSOR", "{\"number\": 1, \"type\": \"GAS\", " + properties + "}").getErrors())
                .containsExactly("SENSOR #1 (number 1): " + error);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '~', value = {
            "\"ha_min_temp\": 30, \"ha_max_temp\": 10     | 'ha_max_temp' must be greater than ha_min_temp, but ha_min_temp is 30.0 and ha_max_temp is 10.0",
            "\"ha_temperature_step\": 0                   | 'ha_temperature_step' must be greater than 0, but is 0.0",
            "\"ha_modes\": \"\"                           | 'ha_modes' needs at least one mode of [auto, off, cool, heat, dry, fan_only]",
            "\"ha_modes\": \"heat,warm\"                  | 'ha_modes' 'warm' is not one of [auto, off, cool, heat, dry, fan_only]",
            "\"ha_fan_modes\": \"low,turbo\"              | 'ha_fan_modes' 'turbo' is not one of [low, medium, high, auto]",
            "\"ha_preset_modes\": \"Dag=DAY,Nacht=NACHT\" | 'ha_preset_modes' 'Nacht=NACHT': unknown preset 'NACHT', expected one of [FROST, DAY, NIGHT, ECO, MANUAL]",
            "\"ha_preset_modes\": \"none=ECO\"            | 'ha_preset_modes' 'none=ECO': Home Assistant reserves the label 'none'",
    })
    void aThermostatIsValidated(String properties, String error) {
        assertThat(invalid("SENSOR", "{\"number\": 1, \"type\": \"TEMPERATURECONTROL\", " + properties + "}").getErrors())
                .containsExactly("SENSOR #1 (number 1): " + error);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '~', value = {
            "\"address_numbers\": \"1\"                             | 'bus_numbers' is required, a comma separated list of numbers like \"1,2\"",
            "\"bus_numbers\": \"1\"                                 | 'address_numbers' is required, a comma separated list of numbers like \"1,2\"",
            "\"bus_numbers\": \"1,x\", \"address_numbers\": \"1,2\" | 'bus_numbers' 'x' in '1,x' is not a number between 0 and 127",
            "\"bus_numbers\": \"1\", \"address_numbers\": \"200\"   | 'address_numbers' '200' in '200' is not a number between 0 and 127",
            "\"bus_numbers\": \"1,1\", \"address_numbers\": \"13\"  | 'address_numbers' needs one address for every bus, but there are 2 bus numbers ('1,1') and 1 address numbers ('13')",
    })
    void aDisplayMessageIsValidated(String properties, String error) {
        assertThat(invalid("DISPLAYMESSAGE", "{\"number\": 1000, " + properties + "}").getErrors())
                .containsExactly("DISPLAYMESSAGE #1 (number 1000): " + error);
    }

    @Test
    void aPulseCounterNeedsPulses() {
        assertThat(invalid("SENSOR", """
                {"number": 6, "type": "PULSECOUNTER", "pulses_per_unit": -1}""").getErrors())
                .containsExactly("SENSOR #1 (number 6): 'pulses_per_unit' must be greater than 0, but is -1");
    }

    @Test
    void theStructureOfTheFileIsValidated() {
        assertThat(invalid(() -> read("{\"componentsTypes\": {\"RELAIS\": [], \"FLAG\": {}}}")).getErrors()).containsExactly(
                "'type' is required, expected one of [PICOS, NANOS, MICROS_PLUS]",
                "componentsTypes: 'RELAIS' is not a function, expected one of [RELAY, DIMMER, MOTOR, LOCMOOD, TIMEDMOOD, GENMOOD, FLAG, SENSOR, COND, INPUT, TIMEDFNC, DISPLAYMESSAGE]",
                "componentsTypes.FLAG must be a list of components, [ ... ]"
        );
        assertThat(invalid(() -> read("{\"type\": \"MEGA\"}")).getErrors()).containsExactly(
                "'type' \"MEGA\" is not a central unit type, expected one of [PICOS, NANOS, MICROS_PLUS]",
                "'componentsTypes' is required, an object with a list of components per function, e.g. {\"RELAY\": [{\"number\": 1, \"description\": \"Kitchen\"}]}"
        );
        assertThat(invalid("RELAY", "1").getErrors()).containsExactly("RELAY #1: must be an object, { ... }");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"type\": \"MICROS_PLUS\",", "[]", ""})
    void aFileThatIsNoJsonObjectIsAnError(String json) {
        assertThat(invalid(() -> read(json)).getErrors()).singleElement().asString().startsWith("The file ");
    }

    @Test
    void invalidJsonTellsWhere() {
        assertThat(invalid(() -> read("{\n  \"type\": \"MICROS_PLUS\"\n  \"componentsTypes\": {}\n}")).getErrors()).singleElement().asString()
                .startsWith("The file is not valid JSON at line 3, column 3: ");
    }

    @Test
    void theTestConfigIsValid() throws IOException {
        try (InputStream config = getClass().getResourceAsStream("/test-config.json")) {
            CentralUnitConfigReader.Result result = CentralUnitConfigReader.read(config, "test-config.json");

            assertThat(result.warnings()).isEmpty();
            assertThat(result.centralUnit().getComponent(Function.SENSOR, 321)).isInstanceOf(GasSensorSpec.class);
        }
    }

    @Test
    void theExampleConfigOfTheReadmeIsValid() throws IOException {
        Matcher example = Pattern.compile("<summary>Example config \\(click me\\)</summary>\\s*```json\\s*(.*?)```", Pattern.DOTALL)
                .matcher(Files.readString(Path.of("README.md")));
        assertThat(example.find()).isTrue();

        assertThat(read(example.group(1)).warnings()).isEmpty();
    }
}
