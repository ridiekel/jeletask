package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

public class StateMapping {
    private final String name;
    private final Number read;
    private final Number write;

    public StateMapping(String name, Number read) {
        this(name, read, read);
    }

    public StateMapping(String name, Number read, Number write) {
        this.name = name;
        this.read = read;
        this.write = write;
    }

    public Number getWrite() {
        return write;
    }

    public String getName() {
        return this.name;
    }

    public Number getRead() {
        return this.read;
    }
}
