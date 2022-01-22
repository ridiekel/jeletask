package io.github.ridiekel.jeletask.client.spec;

public enum CentralUnitType {
    PICOS("Picos"),
    NANOS("Nanos"),
    MICROS("Micros"),
    MICROS_PLUS("Micros Plus");

    private final String displayName;

    CentralUnitType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
