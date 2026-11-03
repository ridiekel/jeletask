package io.github.ridiekel.jeletask.client.spec.component;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.SensorType;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.TemperatureControlStateCalculator.ValidTemperatureControlPreset;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.TemperatureControlStateCalculator.ValidTemperatureControlSpeed;
import io.github.ridiekel.jeletask.client.spec.config.ConfigProblems;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * A thermostat (e.g. an AURUS or an air conditioning unit), shown as a climate in Home Assistant.
 */
@Getter
@Setter
public class TemperatureControlSpec extends DecimalSensorSpec {
    // The HVAC modes of Home Assistant that the mode templates of HASensorConfig can translate to a Teletask mode
    private static final List<String> VALID_MODES = List.of("auto", "off", "cool", "heat", "dry", "fan_only");
    private static final List<String> VALID_FAN_MODES = Arrays.stream(ValidTemperatureControlSpeed.values()).map(s -> s.name().toLowerCase(Locale.ROOT)).toList();
    private static final String VALID_PRESETS = Arrays.toString(ValidTemperatureControlPreset.values());
    private static final String HA_PRESET_MODES = "ha_preset_modes";

    @JsonProperty("ha_temperature_step")
    private double haTemperatureStep = 0.5;

    @JsonProperty("ha_modes")
    private String haModes = String.join(",", VALID_MODES);

    @JsonProperty("ha_min_temp")
    private double haMinTemp = 10;

    @JsonProperty("ha_max_temp")
    private double haMaxTemp = 30;

    // Empty = no fan control in HA (e.g. radiator zones)
    @JsonProperty("ha_fan_modes")
    private String haFanModes = "auto,low,medium,high";

    // Presets shown in HA, comma separated: "PRESET" or "Label=PRESET" (DAY, NIGHT, ECO, FROST, MANUAL). Empty = no presets
    @JsonProperty(HA_PRESET_MODES)
    private String haPresetModes = "";

    // Show the demand of output_state as hvac_action. Only observed on a MICROS+, the manual marks that byte as not used
    @JsonProperty("ha_hvac_action")
    private boolean haHvacAction = false;

    @Override
    public SensorType getSensorType() {
        return SensorType.TEMPERATURECONTROL;
    }

    @Override
    public void validate(ConfigProblems problems) {
        super.validate(problems);
        if (this.haTemperatureStep <= 0) {
            problems.error("ha_temperature_step", "must be greater than 0, but is " + this.haTemperatureStep);
        }
        if (this.haMinTemp >= this.haMaxTemp) {
            problems.error("ha_max_temp", "must be greater than ha_min_temp, but ha_min_temp is " + this.haMinTemp + " and ha_max_temp is " + this.haMaxTemp);
        }
        if (this.haModes == null || this.haModes.isBlank()) {
            problems.error("ha_modes", "needs at least one mode of " + VALID_MODES);
        } else {
            validateValues(problems, "ha_modes", this.haModes, VALID_MODES);
        }
        if (this.haFanModes != null && !this.haFanModes.isBlank()) {
            validateValues(problems, "ha_fan_modes", this.haFanModes, VALID_FAN_MODES);
        }
        this.presetModes((entry, reason) -> problems.error(HA_PRESET_MODES, "'" + entry + "': " + reason));
    }

    private static void validateValues(ConfigProblems problems, String property, String commaSeparated, List<String> valid) {
        Arrays.stream(commaSeparated.split(","))
                .filter(value -> !valid.contains(value))
                .forEach(value -> problems.error(property, "'" + value + "' is not one of " + valid));
    }

    /**
     * label -> Teletask preset, e.g. "Manual=MANUAL,Comfort=DAY". A missing preset in the state means MANUAL.
     * Unknown presets and the label "none" (reserved by HA) are left out and passed to {@code invalid} with the reason, so HA does not reject the whole entity.
     */
    public Map<String, String> presetModes(BiConsumer<String, String> invalid) {
        Map<String, String> presets = new LinkedHashMap<>();
        for (String entry : Optional.ofNullable(this.haPresetModes).orElse("").split(",")) {
            if (!entry.isBlank()) {
                String[] parts = entry.split("=", 2);
                String label = parts[0].trim();
                String preset = (parts.length > 1 ? parts[1] : parts[0]).trim().toUpperCase(Locale.ROOT);
                if (Arrays.stream(ValidTemperatureControlPreset.values()).noneMatch(p -> p.name().equals(preset))) {
                    invalid.accept(entry, "unknown preset '" + preset + "', expected one of " + VALID_PRESETS);
                } else if (label.equalsIgnoreCase("none")) {
                    invalid.accept(entry, "Home Assistant reserves the label 'none'");
                } else {
                    presets.put(label, preset);
                }
            }
        }
        return presets;
    }
}
