package io.github.ridiekel.jeletask.client.spec.component;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.SensorType;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.GasStateCalculator;
import io.github.ridiekel.jeletask.client.spec.config.ConfigProblems;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A General Analog Sensor: the raw value of the signal (e.g. 4-20mA) is scaled to the range between gas_min and gas_max.
 */
@Getter
@Setter
public class GasSensorSpec extends DecimalSensorSpec {
    private static final String GAS_MIN = "gas_min";
    private static final String GAS_MAX = "gas_max";

    @JsonProperty("gas_type")
    private String gasType = "";

    @JsonProperty(GAS_MIN)
    private BigDecimal gasMin = BigDecimal.ZERO;

    @JsonProperty(GAS_MAX)
    private BigDecimal gasMax = BigDecimal.ZERO;

    @Override
    public SensorType getSensorType() {
        return SensorType.GAS;
    }

    @Override
    public void validate(ConfigProblems problems) {
        super.validate(problems);
        if (this.gasType == null || this.gasType.isBlank()) {
            problems.error("gas_type", "is required, expected one of " + GasStateCalculator.GAS_TYPES);
        } else if (!GasStateCalculator.GAS_TYPES.contains(this.gasType)) {
            problems.error("gas_type", "'" + this.gasType + "' is not a gas type, expected one of " + GasStateCalculator.GAS_TYPES);
        }
        if (this.gasMin == null) {
            problems.error(GAS_MIN, "is required");
        }
        if (this.gasMax == null) {
            problems.error(GAS_MAX, "is required");
        }
        if (this.gasMin != null && this.gasMax != null && this.gasMax.compareTo(this.gasMin) <= 0) {
            problems.error(GAS_MAX, "must be greater than gas_min, but gas_min is " + this.gasMin + " and gas_max is " + this.gasMax);
        }
    }
}
