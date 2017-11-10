package be.xhibit.teletask.model.spec;

/**
 * Define all Teletask functions used by the API here.
 */
public enum Function {
    RELAY("relay"),
    DIMMER("dimmer"),
    MOTOR("motor on/off"),
    LOCMOOD("local mood"),
    TIMEDMOOD("timed mood"),
    GENMOOD("general mood"),
    FLAG("flag"),
    SENSOR("sensor value"),
    COND("condition");

    private final String description;

    Function(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }
}
