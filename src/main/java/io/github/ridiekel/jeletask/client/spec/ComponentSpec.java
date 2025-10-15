package io.github.ridiekel.jeletask.client.spec;


import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.config.HAConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a Teletask component, being either a: relay, motor, mood, ... basically anything which can be controlled.
 */
public class ComponentSpec {
    @Setter
    @Getter
    private String description;
    @Setter
    @Getter
    private Function function;
    @Setter
    @Getter
    private int number;
    @Setter
    @Getter
    private State<?> state;
    @Setter
    @Getter

    //type is the type as it is known to teletask
    private String type;

    // Defaulting to 1 for temperature sensors. Otherwise we would have to always override this.
    @Setter
    @Getter
    private int decimals = 1;

    //haType is the type as it is known to Home Assistant
    private String haType;
    @Setter
    @Getter
    private String HA_unit_of_measurement;
    @Setter
    @Getter
    private String HA_modes = "auto,off,cool,heat,dry,fan_only";

    // For GAS (General Analog Sensor)
    @Setter
    @Getter
    private String gas_type = "";
    @Setter
    @Getter
    private float gas_min = 0;
    @Setter
    @Getter
    private float gas_max = 0;

    // For DISPLAYMESSAGE
    @Setter
    private String address_numbers;
    @Setter
    private String bus_numbers;

    // For PULSECOUNTER
    @Setter
    @Getter
    private int pulses_per_unit = 1000;

    // For INPUT
    @Getter
    private Integer long_press_duration_millis;

    @Getter
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
