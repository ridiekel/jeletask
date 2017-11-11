package io.jeletask.teletask.config.model.json;

import io.jeletask.teletask.model.spec.ComponentSpec;
import io.jeletask.teletask.model.spec.Function;

/**
 * This class represents a Teletask component, being either a: relay, motor, mood, ... basically anything which can be controlled.
 */
public class TDSComponent implements ComponentSpec {
    private String description;
    private Function function;
    private int number;
    private String state;
    private String type;

    /**
     * Default constructor.
     * The default constructor is used by Jackson.  In order not to have null values, some fields are initialised to empty strings.
     */
    public TDSComponent() {
        this.description = "";
    }

    /**
     * Constructor taking arguments status, state and number.
     * @param function The function for which the call was requested.
     * @param state The current status of the component, for example 0 indicating off for a "relay".
     * @param number The component number you wish to manipulate.
     */
    public TDSComponent(Function function, String state, int number) {
        this.function = function;
        this.state = state;
        this.number = number;
    }

    @Override
    public int getNumber() {
        return this.number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    @Override
    public String getRoomName() {
        return "N/A";
    }

    @Override
    public String getState() {
        return this.state;
    }

    @Override
    public void setState(String state) {
        this.state = state;
    }

    @Override
    public Function getFunction() {
        return this.function;
    }

    public void setFunction(Function function) {
        this.function = function;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
