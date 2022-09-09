package io.github.ridiekel.jeletask.client.spec.state;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.util.stream.Collectors;

public class ComponentState {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL).setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private String state;
    private String lastDirection;
    private String protection;
    private Number position;
    private Number secondsToFinish;
    private Number correctionAtZeroPercentInSeconds;
    private Number correctionAtHundredPercentInSeconds;

    public ComponentState() {
    }

    public ComponentState(Number state) {
        this.state = String.valueOf(state);
    }

    public ComponentState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getLastDirection() {
        return lastDirection;
    }

    public void setLastDirection(String lastDirection) {
        this.lastDirection = lastDirection;
    }

    public String getProtection() {
        return protection;
    }

    public void setProtection(String protection) {
        this.protection = protection;
    }

    public Number getPosition() {
        return position;
    }

    public void setPosition(Number position) {
        this.position = position;
    }

    public Number getSecondsToFinish() {
        return secondsToFinish;
    }

    public void setSecondsToFinish(Number secondsToFinish) {
        this.secondsToFinish = secondsToFinish;
    }

    public Number getCorrectionAtZeroPercentInSeconds() {
        return correctionAtZeroPercentInSeconds;
    }

    public void setCorrectionAtZeroPercentInSeconds(Number correctionAtZeroPercentInSeconds) {
        this.correctionAtZeroPercentInSeconds = correctionAtZeroPercentInSeconds;
    }

    public Number getCorrectionAtHundredPercentInSeconds() {
        return correctionAtHundredPercentInSeconds;
    }

    public void setCorrectionAtHundredPercentInSeconds(Number correctionAtHundredPercentInSeconds) {
        this.correctionAtHundredPercentInSeconds = correctionAtHundredPercentInSeconds;
    }

    public static ComponentState parse(String state) {
        try {
            return OBJECT_MAPPER.readValue(state, ComponentState.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String prettyString() {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this).lines().map(l -> "           " + l).collect(Collectors.joining("\n"));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
