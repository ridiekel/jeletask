package io.github.ridiekel.jeletask.client.spec.state.impl;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.TemperatureControlStateCalculator;
import io.github.ridiekel.jeletask.client.spec.state.State;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@SuperBuilder
@AllArgsConstructor
public class TemperatureControlState extends State<OnOffToggleStateCalculator.ValidOnOffToggle> {
    private BigDecimal currentTemperature;
    private BigDecimal targetTemperature;
    private BigDecimal dayPresetTemperature;
    private BigDecimal nightAtHeatingPresetTemperature;
    private BigDecimal nightAtCoolingPresetTemperature;
    private BigDecimal ecoPresetOffset;
    private TemperatureControlStateCalculator.ValidTemperatureControlPreset preset;
    private TemperatureControlStateCalculator.ValidTemperatureControlMode mode;
    private TemperatureControlStateCalculator.ValidTemperatureControlSpeed fanspeed;
    private TemperatureControlStateCalculator.ValidTemperatureControlAction action;
    private Number windowOpen;
    private Number swingDirection;
    private Number outputState;

    public TemperatureControlState(OnOffToggleStateCalculator.ValidOnOffToggle state) {
        super(state);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        TemperatureControlState that = (TemperatureControlState) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(currentTemperature, that.currentTemperature)
                .append(targetTemperature, that.targetTemperature)
                .append(dayPresetTemperature, that.dayPresetTemperature)
                .append(nightAtHeatingPresetTemperature, that.nightAtHeatingPresetTemperature)
                .append(nightAtCoolingPresetTemperature, that.nightAtCoolingPresetTemperature)
                .append(ecoPresetOffset, that.ecoPresetOffset)
                .append(preset, that.preset)
                .append(mode, that.mode)
                .append(fanspeed, that.fanspeed)
                .append(windowOpen, that.windowOpen)
                .append(swingDirection, that.swingDirection)
                .append(outputState, that.outputState)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .appendSuper(super.hashCode())
                .append(currentTemperature)
                .append(targetTemperature)
                .append(dayPresetTemperature)
                .append(nightAtHeatingPresetTemperature)
                .append(nightAtCoolingPresetTemperature)
                .append(ecoPresetOffset)
                .append(preset)
                .append(mode)
                .append(fanspeed)
                .append(windowOpen)
                .append(swingDirection)
                .append(outputState)
                .toHashCode();
    }
}
