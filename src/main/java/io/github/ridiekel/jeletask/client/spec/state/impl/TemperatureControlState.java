package io.github.ridiekel.jeletask.client.spec.state.impl;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.spec.state.State;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class TemperatureControlState extends State<OnOffToggleStateCalculator.ValidOnOffToggle> {
    private Number currentTemperature;

    private Number targetTemperature;
    private Number dayPresetTemperature;
    private Number nightAtHeatingPresetTemperature;
    private Number nightAtCoolingPresetTemperature;
    private Number ecoPresetOffset;
    private String preset;
    private String mode;
    private String fanspeed;
    private Number windowOpen;
    private Number swingDirection;
    private Number outputState;

    public TemperatureControlState() {
    }

    public TemperatureControlState(OnOffToggleStateCalculator.ValidOnOffToggle state) {
        super(state);
    }

    public Number getCurrentTemperature() {
        return currentTemperature;
    }

    public void setCurrentTemperature(Number currentTemperature) {
        this.currentTemperature = currentTemperature;
    }

    public Number getTargetTemperature() {
        return targetTemperature;
    }

    public void setTargetTemperature(Number targetTemperature) {
        this.targetTemperature = targetTemperature;
    }

    public Number getDayPresetTemperature() {
        return dayPresetTemperature;
    }

    public void setDayPresetTemperature(Number dayPresetTemperature) {
        this.dayPresetTemperature = dayPresetTemperature;
    }

    public Number getNightAtHeatingPresetTemperature() {
        return nightAtHeatingPresetTemperature;
    }

    public void setNightAtHeatingPresetTemperature(Number nightAtHeatingPresetTemperature) {
        this.nightAtHeatingPresetTemperature = nightAtHeatingPresetTemperature;
    }

    public Number getNightAtCoolingPresetTemperature() {
        return nightAtCoolingPresetTemperature;
    }

    public void setNightAtCoolingPresetTemperature(Number nightAtCoolingPresetTemperature) {
        this.nightAtCoolingPresetTemperature = nightAtCoolingPresetTemperature;
    }

    public Number getEcoPresetOffset() {
        return ecoPresetOffset;
    }

    public void setEcoPresetOffset(Number ecoPresetOffset) {
        this.ecoPresetOffset = ecoPresetOffset;
    }

    public String getPreset() {
        return preset;
    }

    public void setPreset(String preset) {
        this.preset = preset;
    }

    public String getMode() {
        return getState().name();
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getFanspeed() {
        return fanspeed;
    }

    public void setFanspeed(String fanspeed) {
        this.fanspeed = fanspeed;
    }

    public Number getWindowOpen() {
        return windowOpen;
    }

    public void setWindowOpen(Number windowOpen) {
        this.windowOpen = windowOpen;
    }

    public Number getSwingDirection() {
        return swingDirection;
    }

    public void setSwingDirection(Number swingDirection) {
        this.swingDirection = swingDirection;
    }

    public Number getOutputState() {
        return outputState;
    }

    public void setOutputState(Number outputState) {
        this.outputState = outputState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        TemperatureControlState that = (TemperatureControlState) o;

        return new EqualsBuilder().appendSuper(super.equals(o)).append(currentTemperature, that.currentTemperature).append(targetTemperature, that.targetTemperature).append(dayPresetTemperature, that.dayPresetTemperature).append(nightAtHeatingPresetTemperature, that.nightAtHeatingPresetTemperature).append(nightAtCoolingPresetTemperature, that.nightAtCoolingPresetTemperature).append(ecoPresetOffset, that.ecoPresetOffset).append(preset, that.preset).append(mode, that.mode).append(fanspeed, that.fanspeed).append(windowOpen, that.windowOpen).append(swingDirection, that.swingDirection).append(outputState, that.outputState).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(currentTemperature).append(targetTemperature).append(dayPresetTemperature).append(nightAtHeatingPresetTemperature).append(nightAtCoolingPresetTemperature).append(ecoPresetOffset).append(preset).append(mode).append(fanspeed).append(windowOpen).append(swingDirection).append(outputState).toHashCode();
    }
}
