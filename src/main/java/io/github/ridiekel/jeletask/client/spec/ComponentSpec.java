package io.github.ridiekel.jeletask.client.spec;


import io.github.ridiekel.jeletask.client.builder.composer.config.configurables.Sensor;
import io.github.ridiekel.jeletask.client.spec.state.State;
import io.github.ridiekel.jeletask.mqtt.listener.homeassistant.HAConfig;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This class represents a Teletask component, being either a: relay, motor, mood, ... basically anything which can be controlled.
 */
public class ComponentSpec  {
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
    private String type;
    @Setter
    @Getter
    private int decimals = 0;

    private String HAtype;
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
    private String HA_subtype = "button_1";

    @Getter
    private List<HAConfig<?>> haPublishedConfig = new ArrayList<>();

    public final Map<String, String> SensorTypesToHATypes = Map.of(
            "TEMPERATURE", "sensor",
            "LIGHT", "sensor",
            "HUMIDITY", "sensor",
            "GAS", "sensor",
            "TEMPERATURECONTROL", "climate",
            "PULSECOUNTER", "sensor"
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
    public ComponentSpec(Function function, State<?> state, int number) {
        this.function = function;
        this.state = state;
        this.number = number;
    }

    public Sensor getTypeEnum() {
        if(type != null && !type.equals("switch")) {
            return Sensor.valueOf(type);
        }
        return null;
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

    public String getAddressNumbers() { return this.address_numbers; }

    public String getBusNumbers() { return this.bus_numbers; }

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
