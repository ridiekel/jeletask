package io.github.ridiekel.jeletask.client.spec;


import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfig;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a Teletask component, being either a: relay, motor, mood, ... basically anything which can be controlled.
 */
@Setter
@Getter
public class ComponentSpec {
    private String description;
    private Function function;
    private int number;
    private State<?> state;

    //type is the type as it is known to teletask
    private String type;

    // Defaulting to 1 for temperature sensors. Otherwise we would have to always override this.
    private Integer decimals = 1;

    //haType is the type as it is known to Home Assistant
    private String haType;
    private String HA_unit_of_measurement;
    private double HA_temperature_step = 0.5;
    private String HA_modes = "auto,off,cool,heat,dry,fan_only";

    // For GAS (General Analog Sensor)
    private String gas_type = "";
    private BigDecimal gas_min = BigDecimal.ZERO;
    private BigDecimal gas_max = BigDecimal.ZERO;

    // For DISPLAYMESSAGE
    private String address_numbers;
    private String bus_numbers;

    // For PULSECOUNTER
    private int pulses_per_unit = 1000;

    // For INPUT
    private Integer long_press_duration_millis;

    private List<HAConfig<?>> haPublishedConfig = new ArrayList<>();

    /**
     * Default constructor.
     * The default constructor is used by Jackson.  In order not to have null values, some fields are initialised to empty strings.
     */
    public ComponentSpec() {
        this.description = "";
    }

    /**
     * Constructor taking arguments status, state and number.
     *
     * @param function The function for which the call was requested.
     * @param state    The current status of the component, for example 0 indicating off for a "relay".
     * @param number   The component number you wish to manipulate.
     */
    public ComponentSpec(Function function, State<?> state, int number) {
        this.function = function;
        this.state = state;
        this.number = number;
    }

    public String getHAType() {
        return this.haType;
    }

    public void setHAType(String HAtype) {
        this.haType = HAtype;
    }

    public String getAddressNumbers() {
        return this.address_numbers;
    }

    public String getBusNumbers() {
        return this.bus_numbers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComponentSpec that = (ComponentSpec) o;

        if (number != that.number) return false;
        return function == that.function;
    }

    @Override
    public int hashCode() {
        int result = function.hashCode();
        result = 31 * result + number;
        return result;
    }
}
