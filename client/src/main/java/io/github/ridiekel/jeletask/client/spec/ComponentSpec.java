package io.github.ridiekel.jeletask.client.spec;


import io.github.ridiekel.jeletask.client.spec.state.ComponentState;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * This class represents a Teletask component, being either a: relay, motor, mood, ... basically anything which can be controlled.
 */
public class ComponentSpec  {
    private String description;
    private Function function;
    private int number;
    private ComponentState state;
    private String type;
    private int decimals = 0;

    private String HAtype;
    private String HA_unit_of_measurement;
    private String HA_modes = "auto,off,cool,heat,dry,fan_only";
    
    // For GAS (General Analog Sensor)
    private String gas_type = "";
    private float gas_min = 0;
    private float gas_max = 0;

    // For DISPLAYMESSAGE
    private String address_numbers;
    private String bus_numbers;

    public final Map<String, String> SensorTypesToHATypes = Map.of(
            "TEMPERATURE", "sensor",
            "LIGHT", "sensor",
            "HUMIDITY", "sensor",
            "GAS", "sensor",
            "TEMPERATURECONTROL", "climate"
    );

    /**
     * Default constructor.
     * The default constructor is used by Jackson.  In order not to have null values, some fields are initialised to empty strings.
     */
    public ComponentSpec() {
        this.description = "";
    }

    /**
     * Constructor taking arguments status, state and number.
     * @param function The function for which the call was requested.
     * @param state The current status of the component, for example 0 indicating off for a "relay".
     * @param number The component number you wish to manipulate.
     */
    public ComponentSpec(Function function, ComponentState state, int number) {
        this.function = function;
        this.state = state;
        this.number = number;
    }

    public int getNumber() {
        return this.number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public ComponentState getState() {
        if (Objects.equals(this.state, "null")) { //TODO: Check why this sometimes happens
            this.state = null;
        }
        return this.state;
    }

    public void setState(ComponentState state) {
        this.state = state;
    }

    public Function getFunction() {
        return this.function;
    }

    public void setFunction(Function function) {
        this.function = function;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() { return this.type; }

    public void setType(String type) {
        this.type = type;
    }

    public String getHAType() {

        if (this.HAtype == null) {

            if (this.type != null)
                return Optional.ofNullable(this.SensorTypesToHATypes.get(this.type)).orElse(this.type);

            return this.type;

        } else
            return this.HAtype;

    }

    public void setHAType(String HAtype) {
        this.HAtype = HAtype;
    }

    public String getGas_type() { return this.gas_type; }

    public void setGas_type(String gas_type) { this.gas_type = gas_type; }

    public float getGas_min() { return this.gas_min; }

    public void setGas_min(float gas_min) { this.gas_min = gas_min; }

    public float getGas_max() { return this.gas_max; }

    public void setGas_max(float gas_max) { this.gas_max = gas_max; }
    
    public int getDecimals() { return this.decimals; }

    public void setDecimals(int decimals) { this.decimals = decimals; }
    
    public String getAddressNumbers() { return this.address_numbers; }

    public void setAddress_numbers(String numbers) { this.address_numbers = numbers; }

    public String getBusNumbers() { return this.bus_numbers; }

    public void setBus_numbers(String numbers) { this.bus_numbers = numbers; }

    public String getHA_unit_of_measurement() { return this.HA_unit_of_measurement; }

    public void setHA_unit_of_measurement(String HA_unit_of_measurement) { this.HA_unit_of_measurement = HA_unit_of_measurement; }

    public String getHA_modes() { return this.HA_modes; }

    public void setHA_modes(String HA_modes) { this.HA_modes = HA_modes; }

}
