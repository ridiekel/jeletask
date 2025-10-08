package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.mapper;

public class StateMapping<V extends Enum<V>> {
    private final V name;
    private final Number read;
    private final Number write;

    public StateMapping(V name, Number read) {
        this(name, read, read);
    }

    public StateMapping(V name, Number read, Number write) {
        this.name = name;
        this.read = read;
        this.write = write;
    }

    public Number getWrite() {
        return write;
    }

    public V getName() {
        return this.name;
    }

    public Number getRead() {
        return this.read;
    }
}
