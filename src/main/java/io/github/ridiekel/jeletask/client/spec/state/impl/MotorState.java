package io.github.ridiekel.jeletask.client.spec.state.impl;

import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.MotorStateCalculator;
import io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.OnOffToggleStateCalculator;
import io.github.ridiekel.jeletask.client.spec.state.State;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigDecimal;

public class MotorState extends State<MotorStateCalculator.ValidMotorDirectionState> {
    private OnOffToggleStateCalculator.ValidOnOffToggle power;
    private MotorStateCalculator.ValidProtectionState protection;
    private Integer requestedPosition;
    private Integer currentPosition;
    private BigDecimal secondsToFinish;
    private Integer correctionAtZeroPercentInSeconds;
    private Integer correctionAtHundredPercentInSeconds;

    public MotorState(MotorStateCalculator.ValidMotorDirectionState state) {
        super(state);
    }

    public MotorState() {
    }

    public OnOffToggleStateCalculator.ValidOnOffToggle getPower() {
        return power;
    }

    public void setPower(OnOffToggleStateCalculator.ValidOnOffToggle power) {
        this.power = power;
    }

    public MotorStateCalculator.ValidProtectionState getProtection() {
        return protection;
    }

    public void setProtection(MotorStateCalculator.ValidProtectionState protection) {
        this.protection = protection;
    }

    public Integer getRequestedPosition() {
        return requestedPosition;
    }

    public void setRequestedPosition(Integer requestedPosition) {
        if (requestedPosition != null && requestedPosition < 0) {
            this.requestedPosition = 0;
        } else if (requestedPosition != null && requestedPosition > 100) {
            this.requestedPosition = 100;
        } else {
            this.requestedPosition = requestedPosition;
        }
    }

    public Integer getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(Integer currentPosition) {
        this.currentPosition = currentPosition;
    }

    public BigDecimal getSecondsToFinish() {
        return secondsToFinish;
    }

    public void setSecondsToFinish(BigDecimal secondsToFinish) {
        this.secondsToFinish = secondsToFinish;
    }

    public Integer getCorrectionAtZeroPercentInSeconds() {
        return correctionAtZeroPercentInSeconds;
    }

    public void setCorrectionAtZeroPercentInSeconds(Integer correctionAtZeroPercentInSeconds) {
        this.correctionAtZeroPercentInSeconds = correctionAtZeroPercentInSeconds;
    }

    public Integer getCorrectionAtHundredPercentInSeconds() {
        return correctionAtHundredPercentInSeconds;
    }

    public void setCorrectionAtHundredPercentInSeconds(Integer correctionAtHundredPercentInSeconds) {
        this.correctionAtHundredPercentInSeconds = correctionAtHundredPercentInSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        MotorState that = (MotorState) o;

        return new EqualsBuilder().appendSuper(super.equals(o)).append(power, that.power).append(protection, that.protection).append(requestedPosition, that.requestedPosition).append(currentPosition, that.currentPosition).append(secondsToFinish, that.secondsToFinish).append(correctionAtZeroPercentInSeconds, that.correctionAtZeroPercentInSeconds).append(correctionAtHundredPercentInSeconds, that.correctionAtHundredPercentInSeconds).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(power).append(protection).append(requestedPosition).append(currentPosition).append(secondsToFinish).append(correctionAtZeroPercentInSeconds).append(correctionAtHundredPercentInSeconds).toHashCode();
    }
}
