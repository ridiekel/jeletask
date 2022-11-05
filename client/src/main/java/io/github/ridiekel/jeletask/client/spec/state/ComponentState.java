package io.github.ridiekel.jeletask.client.spec.state;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.github.ridiekel.jeletask.client.spec.Function;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ComponentState {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL).setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private String state;

    // For MOTOR
    private String lastDirection;
    private String protection;
    private Number position;
    private Number currentPosition;
    private Number secondsToFinish;
    private Number correctionAtZeroPercentInSeconds;
    private Number correctionAtHundredPercentInSeconds;

    // For DIMMER
    private Number brightness;

    // for TEMPERATURECONTROL
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

    // For DISPLAYMESSAGE
    private String messageLine1;
    private String messageLine2;
    private Number messageBeeps;
    private String messageType;

    // For PULSECOUNTER
    private Number current;
    private Number total;


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
    public void setPosition(Number position) { this.position = position; }

    public Number getCurrentPosition() {
        return currentPosition;
    }
    public void setCurrentPosition(Number position) { this.currentPosition = position; }

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

    public Number getBrightness() {
        return brightness;
    }
    public void setBrightness(Number brightness) {
        this.brightness = brightness;
    }

    public Number getCurrentTemperature() {
        return currentTemperature;
    }
    public void setCurrentTemperature(Number temperature) {
        this.currentTemperature = temperature;
    }
    public Number getTargetTemperature() { return targetTemperature; }
    public void setTargetTemperature(Number temperature) {
        this.targetTemperature = temperature;
    }

    public Number getDayPresetTemperature() { return dayPresetTemperature; }
    public void setDayPresetTemperature(Number temperature) {
        this.dayPresetTemperature = temperature;
    }

    public Number getNightAtHeatingPresetTemperature() { return nightAtHeatingPresetTemperature; }
    public void setNightAtHeatingPresetTemperature(Number temperature) { this.nightAtHeatingPresetTemperature = temperature; }

    public Number getNightAtCoolingPresetTemperature() { return nightAtCoolingPresetTemperature; }
    public void setNightAtCoolingPresetTemperature(Number temperature) { this.nightAtCoolingPresetTemperature = temperature; }

    public Number getEcoPresetOffset() { return ecoPresetOffset; }
    public void setEcoPresetOffset(Number temperature) { this.ecoPresetOffset = temperature; }

    public String getPreset() { return preset; }
    public void setPreset(String preset) {
        this.preset = preset;
    }

    public String getMode() { return mode; }
    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getFanspeed() { return fanspeed; }
    public void setFanspeed(String fanspeed) { this.fanspeed = fanspeed; }

    public Number getWindowOpen() { return windowOpen; }
    public void setWindowOpen(Number windowOpen) { this.windowOpen = windowOpen; }

    public Number getSwingDirection() { return swingDirection; }
    public void setSwingDirection(Number swingDirection) { this.swingDirection = swingDirection; }

    public Number getOutputState() { return outputState; }
    public void setOutputState(Number outputState) { this.outputState = outputState; }

    public String getMessageLine1() { return messageLine1; }
    public void setMessageLine1(String line) {
        this.messageLine1 = line;
    }

    public String getMessageLine2() { return messageLine2; }
    public void setMessageLine2(String line) {
        this.messageLine2 = line;
    }

    public Number getMessageBeeps() { return messageBeeps; }
    public void setMessageBeeps(Number beeps) { this.messageBeeps = beeps; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String type) {
        this.messageType = type;
    }

    public Number getCurrent() { return current; }
    public void setCurrent(Number current) { this.current = current; }
    public Number getTotal() { return total; }
    public void setTotal(Number total) { this.total = total; }

    public static ComponentState parse(Function function, String state) {
        try {
            return OBJECT_MAPPER.readValue(state, ComponentState.class);
        } catch (JsonProcessingException e) {
            if (state != null && SIMPLE_VALUES.containsKey(function) && SIMPLE_VALUES.get(function).containsKey(state)) {
                try {
                    return OBJECT_MAPPER.readValue(SIMPLE_VALUES.get(function).get(state).apply(state), ComponentState.class);
                } catch (Exception ex) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private static final Map<Function, Map<String, java.util.function.Function<String, String>>> SIMPLE_VALUES = new HashMap<>();

    static {
        simpleValueMapper(Function.MOTOR).put("UP", ComponentState::stateTemplate);
        simpleValueMapper(Function.MOTOR).put("DOWN", ComponentState::stateTemplate);
        simpleValueMapper(Function.MOTOR).put("STOP", ComponentState::stateTemplate);
        IntStream.range(0, 101).forEach(i -> simpleValueMapper(Function.MOTOR).put(String.valueOf(i), ComponentState::positionTemplate));

        simpleValueMapper(Function.RELAY).put("ON", ComponentState::stateTemplate);
        simpleValueMapper(Function.RELAY).put("OFF", ComponentState::stateTemplate);

        simpleValueMapper(Function.LOCMOOD).put("ON", ComponentState::stateTemplate);
        simpleValueMapper(Function.LOCMOOD).put("OFF", ComponentState::stateTemplate);

        simpleValueMapper(Function.GENMOOD).put("ON", ComponentState::stateTemplate);
        simpleValueMapper(Function.GENMOOD).put("OFF", ComponentState::stateTemplate);

        simpleValueMapper(Function.DIMMER).put("ON", s -> stateTemplate("100"));
        simpleValueMapper(Function.DIMMER).put("OFF", s -> stateTemplate("0"));
        simpleValueMapper(Function.DIMMER).put("PREVIOUS_STATE", s -> stateTemplate("103"));
        IntStream.range(0, 101).forEach(i -> simpleValueMapper(Function.DIMMER).put(String.valueOf(i), ComponentState::stateTemplate));
        
        simpleValueMapper(Function.TIMEDFNC).put("ON", ComponentState::stateTemplate);        
        simpleValueMapper(Function.TIMEDFNC).put("OFF", ComponentState::stateTemplate);
    }

    private static Map<String, java.util.function.Function<String, String>> simpleValueMapper(Function func) {
        return SIMPLE_VALUES.computeIfAbsent(func, (a) -> new HashMap<>());
    }

    public static String stateTemplate(String state) {
        return template("state", state);
    }

    public static String positionTemplate(String position) {
        return template("position", position);
    }

    public static String template(String key, Object value) {
        return "{\"" + key + "\":\"" + value + "\"}";
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
